#!/bin/sh

PATH=/bin:/usr/bin

# set to 1 to enable debugging
DEBUG=0

## Openshift Project. Will get created if it does not exist
project="continuous-infra"

## List all templates to be processed
templates="fedoraci-runner/fedoraci-runner-buildconfig-template.yml \
jenkins/jenkins-fedoraci-slave-buildconfig-template.yaml"

logerror() {
  echo "Error: $1" >&2
  exit 1
}

logwarning() {
  echo "Warning: $1" >&2
}

logdebug() {
  if [ "${DEBUG}" = "1" ] ; then
    echo "DEBUG: $1" >&2
  fi
}

loginfo() {
  echo "$1"
}

cleanup() {
  [ -z "${ci_pipeline_location:=}" ] || rm -rf "${ci_pipeline_location}"
}

verifyEnv() {
  ## jq
  jq --help >/dev/null 2>&1
  if [ $? -eq 127 ]; then
    echo "Require jq but it's not installed.  Aborting." >&2
    exit 1
  fi

  ## oc
  oc --help >/dev/null 2>&1
  if [ $? -eq 127 ]; then
    echo "Require oc but it's not installed.  Aborting." >&2
    exit 1
  fi
}

processTemplate() {
    templateFile="${1}"
    loginfo "* Processing ${templateFile}..."
    templateName=$(oc process -f "${templateFile}" | jq '.items[1].metadata.labels.template' | sed 's/"//g')
    logdebug "  - Template name is ${templateName}"
    imageStreamName=$(oc process -f "${templateFile}" | jq '.items[0].metadata.name' | sed 's/"//g')
    logdebug "  - ImageStream name is ${imageStreamName}"
    buildConfigName=$(oc process -f "${templateFile}" | jq '.items[1].metadata.name' | sed 's/"//g')
    logdebug "  - Build Config name is ${buildConfigName}"

    if ! oc get template "${templateName}" > /dev/null 2>&1 ; then
        loginfo "    >> Creating Build Config Template ${templateName}"
        oc create -f "${templateFile}" > /dev/null 2>&1 || { echo "Failed to create build config! Aborting." >&2; exit 1; }
    else
        logdebug "    >> Updating Build Config Template ${templateName}"
        oc replace -f "${templateFile}" > /dev/null 2>&1 || { echo "Failed to update build config! Aborting." >&2; exit 1; }
    fi

    imageExists=0
    if oc get imagestream "${imageStreamName}" > /dev/null 2>&1 ; then
        logdebug "    Image Stream ${imageStreamName} already exists"
        imageExists=1
    fi
    buildConfigExists=0
    if oc get buildconfig "${buildConfigName}" > /dev/null 2>&1 ; then
        logdebug "    Build Config ${buildConfigName} already exists"
        buildConfigExists=1
    fi

    if [ ${imageExists} -eq 0 ] && [ ${buildConfigExists} -eq 0 ] ; then
        loginfo "    >> Image Stream and Build Config do not exist. Creating..."
        oc new-app "${templateName}" "${REPO_URL_PARAM}" "${REPO_REF_PARAM}" > /dev/null 2>&1 || { echo "Failed to create new app! Aborting." >&2; exit 1; }
    fi
    loginfo ""
}

##
verifyEnv

loginfo ""

if [ -z "${REPO_URL}" ] ; then
  REPO_URL_PARAM=""
else
  REPO_URL_PARAM="-p REPO_URL=${REPO_URL}"
fi
##
if [ -z "${REPO_REF}" ] ; then
  REPO_REF_PARAM=""
else
  REPO_REF_PARAM="-p REPO_REF=${REPO_REF}"
fi

if ! oc project "${project}" > /dev/null 2>&1 ; then
  logdebug "Project does not exist...Creating..."
  oc new-project "${project}" > /dev/null 2>&1 || { echo "Failed to create new project! Aborting." >&2; exit 1; }
  oc project "${project}" > /dev/null 2>&1
fi

trap cleanup EXIT HUP INT TERM

for template in ${templates}; do
    processTemplate "${template}"
done

loginfo "Done!"
