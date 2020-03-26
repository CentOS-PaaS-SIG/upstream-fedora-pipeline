#!/bin/sh

# This container builds with koji into $RPMDIR starting from a pagure PR

PATH=/bin:/usr/bin

# Check to make sure we have all required vars
if [ -z "${fed_repo:=}" ]; then
    echo "No fed_repo env var" >&2
    exit 1
fi

if [ -z "${fed_id:=}" ]; then
    echo "No fed_id env var" >&2
    exit 1
fi

if [ -z "${fed_uid:=}" ]; then
    echo "No fed_uid env var" >&2
    exit 1
fi

if [ -z "${FEDORA_PRINCIPAL:=}" ]; then
    echo "No FEDORA_PRINCIPAL env var" >&2
    exit 1
fi

if [ -z "${PAGURE_URL:=}" ]; then
    echo "No PAGURE_URL env var" >&2
    exit 1
fi

CURRENTDIR="$(realpath "$(pwd)")"

if [ "${CURRENTDIR}" = "/" ] ; then
    if [ ! -d /home ]; then
        echo "Missing /home" >&2
        exit 1
    fi

    cd /home || exit 1
    CURRENTDIR=/home
fi

# Allow change koji server to be used
KOJI_PARAMS="${KOJI_PARAMS:-}"

RPMDIR="${CURRENTDIR}/${fed_repo}_repo"

# Create one dir to store logs in that will be mounted
LOGDIR="${CURRENTDIR}/logs"
[ -d "${LOGDIR}" ] && rm -rf "${LOGDIR:?}"/*
mkdir -p "${LOGDIR}"

# Clone the fedoraproject git repo
rm -rf "${fed_repo}"

if ! git clone "${PAGURE_URL}"/"${fed_namespace:?}"/"${fed_repo}".git ; then
    retcode=$?
    echo "ERROR: GIT CLONE"
    echo "STATUS: ${retcode}"
    exit 1
fi

cd "${fed_repo}" || exit 1
# Checkout the branch and apply the patch to HEAD of branch
git checkout "${fed_branch:?}"
git fetch -fu origin refs/pull/"${fed_id}"/head:pr
# Setting git config and merge message in case we try to merge a closed PR, like it is done on stage instance
git -c "user.name=Fedora CI" -c "user.email=ci@lists.fedoraproject.org"  merge pr -m "Fedora CI pipeline"
# Get current NVR
truenvr=$(rpm -q --define "dist .$DIST_BRANCH" --queryformat '%{name}-%{version}-%{release}\n' --specfile "${fed_repo}".spec | awk 'NR==1')
echo "original_spec_nvr=${truenvr}" >> ${LOGDIR}/job.props

# Build srpm to send to koji
fedpkg --release "${fed_branch}" srpm
VERSION=$(rpmspec --queryformat "%{VERSION}\n" -q "${fed_repo}".spec | awk 'NR==1')
# Set up koji creds
kinit -k -t "${CURRENTDIR}/fedora.keytab" "${FEDORA_PRINCIPAL}"

 # Some packages are requiring configure not be run as root, so set this to bypass the error
export FORCE_UNSAFE_CONFIGURE=1

# Build the package with koji
# ignore koij exit status. If build fails it will be detected later, https://pagure.io/fedora-ci/general/issue/76
koji "${KOJI_PARAMS}" build --wait --arch-override=x86_64 --scratch "${fed_branch}" "${fed_repo}"*.src.rpm | tee ${LOGDIR}/kojioutput.txt || true

cd "${CURRENTDIR}" || exit 1

SCRATCHID=$(awk '/Created task:/ { print $3 }' < ${LOGDIR}/kojioutput.txt)
if ! echo "${SCRATCHID}" | grep -q -E '^[0-9]+$' >/dev/null 2>&1 ; then
    echo "status=FAIL" >> ${LOGDIR}/job.props
    echo "ERROR: KOJI BUILD"
    exit 1
fi

echo "koji_task_id=${SCRATCHID}" >> ${LOGDIR}/job.props

# Make sure koji build finished
TASK_STATE="unknown"
while echo ${TASK_STATE} | grep -Ev "closed|failed|cancelled"; do
    # Wait for build to finish as the command can exit before build finishes, ignore exit code
    koji watch-task "${SCRATCHID}" || true
    for i in $(seq 1 5); do
        if koji taskinfo "${SCRATCHID}" | tee ${LOGDIR}/taskinfo.txt; then
            break
        fi
        if [ "${i}" -eq 5 ]; then
            echo "status=FAIL" >> ${LOGDIR}/job.props
            echo "ERROR: KOJI TASK_INFO"
            exit 1
        fi
        sleep 60
    done
    TASK_STATE=$(grep "State:" ${LOGDIR}/taskinfo.txt | awk '{print$2}')
done

if [ ! "${TASK_STATE}" = "closed" ]; then
     echo "status=FAIL" >> ${LOGDIR}/job.props
     echo "ERROR: KOJI BUILD"
     echo "STATUS: ${TASK_STATE}"
     exit 1
fi

echo "status=SUCCESS" >> ${LOGDIR}/job.props

# Make repo to download rpms to
rm -rf "${RPMDIR}"
mkdir -p "${RPMDIR}"
# Create repo
cd "${RPMDIR}" || exit 1
for i in $(seq 1 5); do
    koji "${KOJI_PARAMS}" download-build --arch=x86_64 --arch=src --arch=noarch --debuginfo --task-id "${SCRATCHID}" || koji "${KOJI_PARAMS}" download-task --arch=x86_64 --arch=src --arch=noarch --logs "${SCRATCHID}" && break
    echo "koji build download failed, attempt: ${i}/5"
    if [ "${i}" -lt 5 ]; then
        sleep 10
    else
        exit 1
    fi
done
createrepo .
cd "${CURRENTDIR}" || exit 1

# Store modified nvr as well
set +e
RPM_TO_CHECK=$(find "${RPMDIR}"/ -name "${fed_repo}-${VERSION}*" | awk 'NR==1')
NVR=$(rpm --queryformat "%{NAME}-%{VERSION}-%{RELEASE}\n" -qp "${RPM_TO_CHECK}")
echo "nvr=${NVR}" >> ${LOGDIR}/job.props
exit 0
