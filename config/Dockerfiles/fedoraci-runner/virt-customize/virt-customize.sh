#!/usr/bin/sh

set -xeuo pipefail

# A shell script that pulls the latest Fedora cloud build
# and uses virt-customize to inject rpms into it. It
# outputs a new qcow2 image for you to use.

CURRENTDIR=$(pwd)

if [ ${CURRENTDIR} == "/" ] ; then
    cd /home
    CURRENTDIR=/home
fi
mkdir -p ${CURRENTDIR}/logs

# Start libvirtd
mkdir -p /var/run/libvirt
libvirtd &
sleep 5
virtlogd &

chmod 666 /dev/kvm

namespace=${namespace:-"rpms"}

if [ $branch != "rawhide" ]; then
    branch=${branch:1}
fi

if [ "${branch}" == "rawhide" ]; then
    curl --connect-timeout 5 --retry 5 --retry-delay 0 --retry-max-time 60 \
         -L -k -O "https://jenkins-continuous-infra.apps.ci.centos.org/job/fedora-rawhide-image-test/lastSuccessfulBuild/artifact/Fedora-Rawhide.qcow2"
    DOWNLOADED_IMAGE_LOCATION="$(pwd)/Fedora-Rawhide.qcow2"
elif [ "${branch}" -ge 28 ]; then
    curl --connect-timeout 5 --retry 5 --retry-delay 0 --retry-max-time 60 \
         -L -k -O "https://jenkins-continuous-infra.apps.ci.centos.org/job/fedora-f${branch}-image-test/lastSuccessfulBuild/artifact/Fedora-${branch}.qcow2"
    DOWNLOADED_IMAGE_LOCATION="$(pwd)/Fedora-${branch}.qcow2"
else
    INSTALL_URL="https://dl.fedoraproject.org/pub/fedora/linux/releases/${branch}/CloudImages/x86_64/images/"
    wget --retry-connrefused --waitretry=1 --read-timeout=20 --timeout=15 --tries 5 --quiet -r --no-parent -A 'Fedora-Cloud-Base*.qcow2' ${INSTALL_URL}
    DOWNLOADED_IMAGE_LOCATION=$(pwd)/$(find dl.fedoraproject.org -name "*.qcow2" | head -1)
fi

function clean_up {
  set +e
  pushd ${CURRENTDIR}/images
  cp ${DOWNLOADED_IMAGE_LOCATION} .
  ln -sf $(find . -name "*.qcow2" | head -1) test_subject.qcow2
  popd
  kill $(jobs -p)
}
trap clean_up EXIT SIGHUP SIGINT SIGTERM

{ #group for tee

mkdir -p ${CURRENTDIR}/images

# Check if the downloaded qcow2 image is valid
qemu-img check ${DOWNLOADED_IMAGE_LOCATION}

# Make dir for just rpm content
mkdir -p ${CURRENTDIR}/testrepo/${package}
# Do there is no packages to copy when running for tests namespace
if [ "${namespace}" != "tests" ]; then
    cp -rp ${rpm_repo}/*.rpm ${rpm_repo}/repodata ${CURRENTDIR}/testrepo/${package}
fi

RPM_LIST=""
function create_repo_file {
cat <<EOF > ${CURRENTDIR}/test-${1}.repo
[test-${1}]
name=test-${1}
baseurl=file:///etc/yum.repos.d/${1}
priority=0
enabled=1
gpgcheck=0
EOF
}

create_repo_file ${package}

gpgcheck=1
if [ "${branch}" != "rawhide" ]; then
    if ! virt-customize --selinux-relabel --memsize 4096 -a ${DOWNLOADED_IMAGE_LOCATION} --run-command "dnf config-manager --set-enable updates-testing updates-testing-debuginfo" ; then
        echo "failure enabling updates-testing repo"
        exit 1
    fi
else
    # Don't check GPG key when testing on Rawhide
    virt-customize -a ${DOWNLOADED_IMAGE_LOCATION} --run-command "sed -i s/gpgcheck=.*/gpgcheck=0/ /etc/yum.repos.d/*.repo"
    gpgcheck=0
fi

koji_repo=$(echo ${DIST_BRANCH}-build | sed -e s'/fc/f/')
# Add repo from latest packages built in koji
cat <<EOF > ${CURRENTDIR}/koji-latest.repo
[koji-${koji_repo}]
name=koji-${koji_repo}
baseurl=https://kojipkgs.fedoraproject.org/repos/${koji_repo}/latest/x86_64/
enabled=1
gpgcheck=0
EOF

virt_copy_files="${CURRENTDIR}/testrepo/${package} ${CURRENTDIR}/test-${package}.repo ${CURRENTDIR}/koji-latest.repo /etc/yum.repos.d/"
# If virt-customize.sh is running as part of PR on tests namespace there is no package built, therefore /testrepo/${package} does not exist
if [ "${namespace}" == "tests" ]; then
    virt_copy_files="${CURRENTDIR}/koji-latest.repo /etc/yum.repos.d/"
fi

if [ -e ${CURRENTDIR}/additional_tasks_repo ]; then
    create_repo_file additional_tasks_repo

    virt_copy_files="${CURRENTDIR}/additional_tasks_repo ${CURRENTDIR}/test-additional_tasks_repo.repo ${virt_copy_files}"
fi

virt-copy-in -a ${DOWNLOADED_IMAGE_LOCATION} ${virt_copy_files} /etc/yum.repos.d/

# Get a list of conflicts from packages already installed in the image
for i in {1..5}; do
    virt-customize -a ${DOWNLOADED_IMAGE_LOCATION} --run-command 'dnf repoquery -q --conflict `rpm -qa --qf "%{NAME} "` > /tmp/conflicts.txt' && break
    if [[ $i -lt 5 ]]; then
        echo "failed to get conflict of installed packages: $i/5"
        sleep 10
    else
        echo "FAIL: failed to get conflict of installed packages"
        exit 1
    fi
done
INSTALLED_CONFLICT_CAPABILITIES=$(virt-cat -a ${DOWNLOADED_IMAGE_LOCATION} /tmp/conflicts.txt)
if [ ! -z "${INSTALLED_CONFLICT_CAPABILITIES}" ] ; then
    SAVEIFS=$IFS
    IFS=$'\n'
    installed_conflicts=""
    # from the possible conflicts get a list of packages would cause conflict
    for installed_conflict_cap in ${INSTALLED_CONFLICT_CAPABILITIES}; do
        installed_conflict=$(dnf repoquery -q --qf "%{NAME}" --disablerepo=\* --enablerepo=${package} --repofrompath=${package},${rpm_repo} --whatprovides "${installed_conflict_cap}")
        if [ ! -z "${installed_conflict}" ]; then
            installed_conflicts="${installed_conflicts} ${installed_conflict}"
        fi
    done
    IFS=$SAVEIFS
fi

# Do install any package if it is tests namespace
if [ "${namespace}" != "tests" ]; then
    for pkg in $(repoquery -q --disablerepo=\* --enablerepo=${package} --repofrompath=${package},${rpm_repo} --all --qf="%{ARCH}:%{NAME}" | sed -e "/^src:/d;/-debug\(info\|source\)\$/d;s/.\+://" | sort -u) ; do
        # check if this package conflicts with any other package from RPM_LIST
        conflict_capabilities=$(repoquery -q --disablerepo=\* --enablerepo=${package} --repofrompath=${package},${rpm_repo} --conflict $pkg)
        conflict_pkgs=''
        if [ -n "${conflict_capabilities}" ] ; then
            for conflict_capability in ${conflict_capabilities}; do
                conflict_capability_pkgs=$(repoquery -q --qf "%{NAME}" --disablerepo=\* --enablerepo=${package} --repofrompath=${package},${rpm_repo} --whatprovides "$conflict_capability")
                if [ -z ${conflict_capability_pkgs} ]; then
                    continue
                fi
                for conflict_capability_pkg in ${conflict_capability_pkgs}; do
                    conflict_pkgs="${conflict_pkgs} ${conflict_capability_pkg}"
                done
            done
        fi
        found_conflict=0
        if [ ! -z "${conflict_pkgs}" ] && [ ! -z "${RPM_LIST}" ]; then
            for rpm_pkg in ${RPM_LIST} ; do
                for conflict in ${conflict_pkgs} ; do
                    if [ "${conflict}" == "$rpm_pkg" ]; then
                        # this pkg conflicts with a package already in RPM_LIST
                        found_conflict=1
                        continue
                    fi
                done
            done
            if [ ${found_conflict} -eq 1 ]; then
                echo "INFO: will not install $pkg as it conflicts with $conflict_pkgs."
                continue
            fi
        fi
        for conflict in ${installed_conflicts}; do
            if [ "${conflict}" == "$pkg" ]; then
                # this pkg conflicts with a package already installed
                found_conflict=1
                continue
            fi
        done
        if [ ${found_conflict} -eq 1 ]; then
            echo "INFO: will not install $pkg as it conflicts with installed package."
            continue
        fi
        RPM_LIST="${RPM_LIST} ${pkg}"
    done
    if [ -z "${RPM_LIST}" ]; then
        echo "FAIL: Failure couldn't find any rpm to install"
        exit 1
    fi
    if ! virt-customize -v --selinux-relabel --memsize 4096 -a ${DOWNLOADED_IMAGE_LOCATION} --run-command "dnf install -y --best --allowerasing --nogpgcheck ${RPM_LIST} && dnf clean all" ; then
        echo "failure installing rpms"
        exit 1
    fi
fi

} 2>&1 | tee ${CURRENTDIR}/logs/console.log #group for tee
