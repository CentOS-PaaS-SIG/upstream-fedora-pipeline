#!/bin/sh

# This script ensures that $rpm_repo actually
# contains an installable $fed_repo rpm

set -xe
PATH=/bin:/usr/bin

if [ -z "${fed_repo:=}" ]; then
    echo "This container requires fed_repo to be defined. Exiting..." >&2
    exit 1
fi

if [ -z "${rpm_repo}" ]; then
    rpm_repo=$(pwd)/${fed_repo}_repo
fi

# Get output to see if any rpms exist
output=$(dnf repoquery --disablerepo=\* --enablerepo="${fed_repo}" --repofrompath="${fed_repo}","${rpm_repo}" --nvr "${fed_repo}")

if [ -z "${output}" ]; then
    echo "No installable rpms found! Failing" >&2
    exit 1
else
    exit 0
fi
