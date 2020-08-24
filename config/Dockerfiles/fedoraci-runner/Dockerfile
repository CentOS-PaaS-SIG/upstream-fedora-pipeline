FROM fedora:latest
LABEL maintainer "https://github.com/CentOS-PaaS-SIG/upstream-fedora-pipeline"
LABEL description="This container is meant to \
contain all script needs to prepare the QCOW2 used as test subject \
fo the Fedora-CI pipeline and also run the tests."
USER root

# Install all package requirements
RUN for i in {1..5} ; do dnf -y install \
        ansible \
        authconfig \
        createrepo \
        dnf-plugins-core \
        dnf-utils \
        dnsmasq \
        fedpkg \
        file \
        findutils \
        fonts-rpm-macros \
        git \
        koji \
        krb5-workstation \
        libguestfs \
        libguestfs-tools-c \
        libvirt-client \
        libvirt-daemon \
        libvirt-daemon-driver-* \
        libvirt-daemon-kvm \
        pykickstart \
        python3-libselinux \
        python3-pip \
# install python3-devel as workaround for https://pagure.io/standard-test-roles/issue/313
        python3-devel \
        python3-dnf \
        python3-sortedcontainers \
        python3-six \
        python3-pyyaml \
        rsync \
        qemu-img \
        qemu-kvm \
        rpm-build \
        standard-test-roles \
        standard-test-roles-inventory-qemu \
        sudo \
        wget \
        && dnf clean all \
        && break || sleep 10 ; done

# Change the anongiturl for fedpkg
# See https://bugzilla.redhat.com/show_bug.cgi?id=1495378
# and https://pagure.io/fedpkg/issue/145
#
RUN sed -i 's@anongiturl.*$@anongiturl = https://src.fedoraproject.org/%(module)s@g' /etc/rpkg/fedpkg.conf

COPY default.xml /etc/libvirt/qemu/networks/
ENV LIBGUESTFS_BACKEND=direct

VOLUME [ "/sys/fs/cgroup" ]

# Copy necessary virt-customize files into container
COPY ["virt-customize/virt-customize.sh", \
# Copy necessary rpmbuild files into the container
      "rpmbuild/pull_old_task.sh", "rpmbuild/repoquery.sh", "rpmbuild/koji_build_pr.sh", \
# Copy necessary files from package-test to the container
      "package-test/package-test.sh", "package-test/verify-rpm.sh", "package-test/rpm-verify.yml", \
      "package-test/resize-qcow2.sh", "package-test/sync-artifacts.yml", \
      "/tmp/"]

# Ansible API changes very often, make sure we run with a version we know it works
RUN pip-3 install ansible==2.8.0

ENV ANSIBLE_INVENTORY=/usr/share/ansible/inventory/standard-inventory-qcow2

ENTRYPOINT ["bash"]
#
# Run the container as follows
# docker run --privileged -v /log/parent/dir:/home -e fed_repo=${packagename} -e fed_branch=${fed_branch} -e fed_rev=${fed_rev} -e RSYNC_PASSWORD=${rsync_password} HTTP_BASE="${HTTP_BASE}" -e RSYNC_USER="${RSYNC_USER}" -e RSYNC_SERVER="${RSYNC_SERVER}" -e RSYNC_DIR="${RSYNC_DIR}" container_tag
