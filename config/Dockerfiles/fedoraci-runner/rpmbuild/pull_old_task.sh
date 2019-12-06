#!/bin/bash

set -xe

# Ensure we have required variable
if [ -z "${PROVIDED_KOJI_TASKID}" ]; then echo "No task id variable provided" ; exit 1 ; fi

CURRENTDIR=$(pwd)
if [ ${CURRENTDIR} == "/" ] ; then
    cd /home
    CURRENTDIR=/home
fi

LOGDIR=${CURRENTDIR}/logs
rm -rf ${LOGDIR}/*
mkdir ${LOGDIR}

# Allow change koji server to be used
KOJI_PARAMS=${KOJI_PARAMS:-}

# Create trap function to archive as many of the variables as we have defined
function archive_variables {
    set +e
    cat << EOF > ${LOGDIR}/job.props
koji_task_id=${PROVIDED_KOJI_TASKID}
fed_repo=${PACKAGE}
fed_rev=kojitask-${PROVIDED_KOJI_TASKID}
nvr=${NVR}
original_spec_nvr=${NVR}
rpm_repo=${RPMDIR}
EOF
rm -rf somewhere
}
trap archive_variables EXIT SIGHUP SIGINT SIGTERM

mkdir somewhere
pushd somewhere
# Download koji build so we can archive it
for i in {1..5}; do
    koji ${KOJI_PARAMS} download-build --arch=x86_64 --arch=src --arch=noarch --debuginfo --task-id ${PROVIDED_KOJI_TASKID} || koji ${KOJI_PARAMS} download-task --arch=x86_64 --arch=src --arch=noarch --logs ${PROVIDED_KOJI_TASKID} && break
    echo "koji build download failed, attempt: $i/5"
    if [[ $i -lt 5 ]]; then
        sleep 10
    else
        exit 1
    fi
done
createrepo .
PACKAGE=$(rpm --queryformat "%{NAME}\n" -qp *.src.rpm)
NVR=$(rpm --queryformat "%{NAME}-%{VERSION}-%{RELEASE}\n" -qp *.src.rpm)
popd

# check if need to create extra repo for group build
if [ -n "${ADDITIONAL_TASK_IDS:-}" ]; then
    mkdir ${CURRENTDIR}/additional_tasks_repo
    pushd ${CURRENTDIR}/additional_tasks_repo
    for additional_taskid in ${ADDITIONAL_TASK_IDS}; do
        # Download koji build so we can archive it
        for i in {1..5}; do
            koji download-build --noprogress --arch=x86_64 --arch=src --arch=noarch --debuginfo --task-id ${additional_taskid} || koji download-task --arch=x86_64 --arch=src --arch=noarch --logs ${additional_taskid} && break
            echo "koji additional task download failed, attempt: $i/5"
            if [[ $i -lt 5 ]]; then
                sleep 10
            else
                echo "koji additional task download failed!"
                exit 1
            fi
        done
    done
    createrepo .
    popd
fi

RPMDIR=${CURRENTDIR}/${PACKAGE}_repo
rm -rf ${RPMDIR}
mkdir -p ${RPMDIR}

mv somewhere/* ${RPMDIR}/

archive_variables
