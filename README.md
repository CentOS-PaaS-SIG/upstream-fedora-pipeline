<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [All Packages Fedora Pipeline Overview](#all-packages-fedora-pipeline-overview)
- [Fedora Atomic Host Pipeline Stages](#all-packages-fedora-pipeline-stages)
  - [Trigger](#trigger)
  - [Build Package](#build-package)
  - [Compose cloud qcow2 image](#compose-cloud-qcow2-image)
  - [Functional Tests on Packages](#functional-tests-on-packages)
- [fedmsg Bus](#fedmsg-bus)
  - [fedmsg - Message Types](#fedmsg---message-types)
    - [fedmsg - Message Legend](#fedmsg---message-legend)
  - [Trigger - org.fedoraproject.prod.git.receive](#trigger---orgfedoraprojectprodgitreceive)
  - [Dist-git message example](#dist-git-message-example)
  - [org.centos.prod.ci.pipeline.allpackages-build.package.queued](#orgcentosprodcipipelineallpackages-buildpackagequeued)
  - [org.centos.prod.ci.pipeline.allpackages-build.package.running](#orgcentosprodcipipelineallpackages-buildpackagerunning)
  - [org.centos.prod.ci.pipeline.allpackages-build.package.complete](#orgcentosprodcipipelineallpackages-buildpackagecomplete)
  - [org.centos.prod.ci.pipeline.allpackages-build.image.queued](#orgcentosprodcipipelineallpackages-buildimagequeued)
  - [org.centos.prod.ci.pipeline.allpackages-build.image.running](#orgcentosprodcipipelineallpackages-buildimagerunning)
  - [org.centos.prod.ci.pipeline.allpackages-build.image.complete](#orgcentosprodcipipelineallpackages-buildimagecomplete)
  - [org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.queued](#orgcentosprodcipipelineallpackages-buildpackagetestfunctionalqueued)
  - [org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.running](#orgcentosprodcipipelineallpackages-buildpackagetestfunctionalrunning)
  - [org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.complete](#orgcentosprodcipipelineallpackages-buildpackagetestfunctionalcomplete)
  - [org.centos.prod.ci.pipeline.allpackages-build.complete](#orgcentosprodcipipelineallpackages-buildcomplete)
  - [org.centos.prod.ci.pipeline.allpackages-pr.package.queued](#orgcentosprodcipipelineallpackages-prpackagequeued)
  - [org.centos.prod.ci.pipeline.allpackages-pr.package.running](#orgcentosprodcipipelineallpackages-prpackagerunning)
  - [org.centos.prod.ci.pipeline.allpackages-pr.package.complete](#orgcentosprodcipipelineallpackages-prpackagecomplete)
  - [org.centos.prod.ci.pipeline.allpackages-pr.image.queued](#orgcentosprodcipipelineallpackages-primagequeued)
  - [org.centos.prod.ci.pipeline.allpackages-pr.image.running](#orgcentosprodcipipelineallpackages-primagerunning)
  - [org.centos.prod.ci.pipeline.allpackages-pr.image.complete](#orgcentosprodcipipelineallpackages-primagecomplete)
  - [org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.queued](#orgcentosprodcipipelineallpackages-prpackagetestfunctionalqueued)
  - [org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.running](#orgcentosprodcipipelineallpackages-prpackagetestfunctionalrunning)
  - [org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.complete](#orgcentosprodcipipelineallpackages-prpackagetestfunctionalcomplete)
  - [org.centos.prod.ci.pipeline.allpackages-pr.complete](#orgcentosprodcipipelineallpackages-prcomplete)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# All Packages Fedora Pipeline Overview

The All Packages Fedora Pipeline serves to test all commits to dist-git for Fedora packages. The pipeline watches all branches corresponding to major Fedora releases and Rawhide. The pipeline begins by submitting a koji scratch build for the new change. Once that build is finished, the pipeline uses imagefactory to construct a cloud qcow2 image. The pipeline adds the newly created rpm at build time to the qcow2 image. Once the image generation is complete, the pipeline runs any tests that are included in the package's dist-git repo, as defined by the standard-test-roles. All stages of the pipeline send out messages on fedmsg for their status.<br><br>

# All Packages Fedora Pipeline Stages

## Trigger

Once packages are pushed to Fedora dist-git this will trigger a message.  The pipeline will be triggered via the [Jenkins JMS plugin](https://wiki.jenkins-ci.org/display/JENKINS/JMS+Messaging+Plugin) for dist-git messages on fedmsg.
Only changes pushed to a fXX or master branch in dist-git are monitored.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.package.[queued,ignored].

## Build Package

The pipeline job begins by submitting a new scratch build to koji for the rpm. Once the koji build is complete, the artifacts, including logs, are downloaded to the Jenkins workspace to be used by the pipeline build and stored as artifacts in Jenkins.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.package.[running,complete].

## Compose cloud qcow2 image

The pipeline next uses virt-customize to grab the latest built qcow2 image for the Fedora major release and install the newly built rpms on the host.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.image.[queued,running,complete].

## Functional Tests on Packages

Functional tests will be executed on the produced package from the previous stage of the pipeline if they exist.  This will help identify issues isolated to the package themselves.  Success or failure will result with a fedmsg back to the Fedora package maintainer. The tests are pulled from the dist-git repos and are executed with the standard-test-roles. If no tests exist, this stage is skipped.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.package.test.functional.[queued,running,complete].

# fedmsg Bus

Communication between Fedora, CentOS, and Red Hat infrastructures will be done via fedmsg.  Messages will be received of updates to Fedora dist-git repos.  Triggering will happen from Fedora dist-git. The pipeline in CentOS infrastructure will build packages, compose a cloud qcow2 image, and run the standard test roles standard tests from the package's dist-git.  We are dependant on CentOS Infrastructure for allowing us a hub for publishing messages to fedmsg.

## fedmsg - Message Types
Below are the different message types that we listen and publish.  There will be different subtopics so we can keep things organized under the org.centos.prod.ci.pipeline.allpackages.* umbrella. The fact that ‘org.centos’ is contained in the messages is a side effect of the way fedmsg enforces message naming. The messages for the build pipelines use allpackages-build in the topic and the messages for the pull request pipelines use allpackages-pr in the topic.

### fedmsg - Message Legend

* build_id - Jenkins pipeline build ID
  - ex. 91
* build_url - Full url to the blue ocean Jenkins pipeline build view
  - ex. https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-pipeline/detail/upstream-fedora-pipeline/226/
* username - Person who made the commit to dist-git
  - ex. eseyman
* rev - This is the commit SHA-1 that is passed on from the dist-git message we recieve
  - ex. 591b0d2fc67a45e4ad13bdc3e312d5554852426a
* namespace - Packaging type passed from dist-git message
  - ex. rpms
* original_spec_nvr - The NVR of the RPM (same as nvr)
  - ex. gnutls-3.5.15-1.fc26
* repo - Package name
  - ex. vim
* topic - Topic that is being published on the fedmsg bus
  - ex. org.centos.prod.ci.pipeline.allpackages.image.complete
* status - Status of the stage and overall pipeline at the time when the message is published.
           UNSTABLE status indicates that some tests did not pass and should be analyzed.
  - ex. SUCCESS
  - options = <SUCCESS/FAILURE/UNSTABLE/ABORTED>
* branch - Fedora branch master = rawhide for now this may change in the future
  - ex. f26
* type - Image type
  - ex. qcow2
* test_guidance - <comma-separated-list-of-test-suites-to-run> required by downstream CI
  - ex. "''"
* comment_id - For pipeline runs triggered by PR comment activity, that comment's unique id
  - ex. 1234
* ref - Indication of what we are building distro/branch/arch/distro_type
  - ex. x86_64
* scratch - Is this message for a scratch build?
  - ex. false
* msgJson - A full dump of the message properties in one field of JSON


Each change passing through the pipeline is uniquely identified by repo, rev, and namespace.

## Trigger - org.fedoraproject.prod.git.receive

Below is an example of the message that we will trigger off of to start our CI pipeline.  We concentrate on the commit part of the message.

````
username=jchaloup
stats={u'files': {u'fix-rootScopeNaming-generate-selfLink-issue-37686.patch': {u'deletions': 8, u'additions': 8, u'lines': 16}, u'build-with-debug-info.patch': {u'deletions': 8, u'additions': 8, u'lines': 16}, u'get-rid-of-the-git-commands-in-mungedocs.patch': {u'deletions': 25, u'additions': 0, u'lines': 25}, u'kubernetes.spec': {u'deletions': 13, u'additions': 11, u'lines': 24}, u'remove-apiserver-add-kube-prefix-for-hyperkube-remov.patch': {u'deletions': 0, u'additions': 169, u'lines': 169}, u'.gitignore': {u'deletions': 1, u'additions': 1, u'lines': 2}, u'sources': {u'deletions': 1, u'additions': 1, u'lines': 2}, u'remove-apiserver-add-kube-prefix-for-hyperkube.patch': {u'deletions': 66, u'additions': 0, u'lines': 66}, u'use_go_build-is-not-fully-propagated-so-make-it-fixe.patch': {u'deletions': 5, u'additions': 5, u'lines': 10}, u'Hyperkube-remove-federation-cmds.patch': {u'deletions': 118, u'additions': 0, u'lines': 118}, u'fix-support-for-ppc64le.patch': {u'deletions': 9, u'additions': 9, u'lines': 18}}, u'total': {u'deletions': 254, u'files': 11, u'additions': 212, u'lines': 466}}
name=Jan Chaloupka
namespace=rpms
rev=b0ef5e0207cea46836a49cd4049908f14015ed8d
agent=jchaloup
summary=Update to upstream v1.6.1
repo=kubernetes
branch=f26
path=/srv/git/repositories/rpms/kubernetes.git
seen=False
message=Update to upstream v1.6.1- related: #1422889
email=jchaloup@redhat.com
````

## Dist-git message example
````
{
  "source_name": "datanommer",
  "i": 1,
  "timestamp": 1493386183.0,
  "msg_id": "2017-b29fa2b4-0600-4f08-9475-5f82f6684bd4",
  "topic": "org.fedoraproject.prod.git.receive",
  "source_version": "0.6.5",
  "signature": "MbQSb1uwzh6UIFKVm+Uxt+56nW/QRH1nOehifxUrbZfiEDEscRdHtb8dj1Skdv7fcZGHhNlR3PGI\nz/4YqPFJjoAM/k60FsnBIIG1gklJaFBM8MloEYauzo/fUK//W99ojk3UPK0lGTIBijG2knbD9t3T\nUMRuDjt45zmGBXHPlR8=\n",
  "msg": {
    "commit": {
      "username": "trasher",
      "stats": {
        "files": {
          "sources": {
            "deletions": 1,
            "additions": 1,
            "lines": 2
          },
          "php-simplepie.spec": {
            "deletions": 5,
            "additions": 8,
            "lines": 13
          },
          ".gitignore": {
            "deletions": 0,
            "additions": 1,
            "lines": 1
          }
        },
        "total": {
          "deletions": 6,
          "files": 3,
          "additions": 10,
          "lines": 16
        }
      },
      "name": "Johan Cwiklinski",
      "rev": "81e09b9c83e8550b54a64c7bdb4e5d7b534df058",
      "namespace": "rpms",
      "agent": "trasher",
      "summary": "Last upstream release",
      "repo": "php-simplepie",
      "branch": "f24",
      "seen": false,
      "path": "/srv/git/repositories/rpms/php-simplepie.git",
      "message": "Last upstream release\n",
      "email": "johan@x-tnd.be"
    }
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.package.queued

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527246787.0, 
  "msg_id": "2018-408adcaa-53e3-4bfa-becf-7736885d3bd0", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.queued", 
  "headers": {}, 
  "signature": "vxbcfp/gWPb1mGkoZ7E+k/EVFPgDNbuvWAboavPk5mKq8Ak5AHmw9jxYHLWvUNq6fkRFiIEUp8Bs\nJteMpWz9+TP8v9v+3dzcv5wxfL4ZfesZwsZg/2rjQLmBa/ZJvUyy6fxUxxoPo0HtBkWAafjNB2Id\nM5m1bVmJZlSziw0q5dE=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "2961", 
    "original_spec_nvr": "", 
    "username": "null", 
    "nvr": "", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-build-pipeline-trigger/detail/fedora-build-pipeline-trigger/2961/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-build-pipeline-trigger", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.queued", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.package.running

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527246833.0, 
  "msg_id": "2018-a27f5cd7-5537-4d4b-ae90-8d787d193654", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.running", 
  "headers": {}, 
  "signature": "TJ3Y4u1mFMxREAW7WnATKurjTVk5qX5gOKTRWEmRo3jBlHHodP1Y/1TehSeKAuIAwEzODs52uYkC\nhFsPFvZt+YLs73DH2hmfbirUaX9/uN9GHRaNYo6z9I0LD4NsMQx4pM7/iapwxkPoXGxcL1QFF551\ngYpceIPJcjA30Wp3ZUE=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "", 
    "username": "null", 
    "nvr": "", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.running", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.package.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527246858.0, 
  "msg_id": "2018-5efbca78-7d3c-41e1-8127-dd225dd8a909", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.complete", 
  "headers": {}, 
  "signature": "PQCKbCLgG/WvhKiey3QJ6rWfBwXPE3hZcI6q3Na75nCnENz0BAyvKTvgpI1ZmEQF1ZiuJZJoNuRd\nWvuuSf4v/18Jsa9osjsleJBwrP4v5s4fHzDZ/lXe0dAtkCk4FemQUN/cMHNrvkPT2TYPvJn6f01K\neQEj7FKtZQ8Zyj2agdI=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.image.queued

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527246863.0, 
  "msg_id": "2018-ce93a867-33d1-4d2b-826e-c149b8fde758", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.image.queued", 
  "headers": {}, 
  "signature": "aBqmzgfiHxsP5i5PUrhDf8ZeeVGqBbq/Ayn2Umjsg+LYSc8gWHg5kcNaSCBkDPFA2Py4SuO/G5yY\nRVDZ1kPx1B592EcbqlIc0nCEVl7SGJZdfDJBotFZUoyasLW1w4Vo88YDahgH+cK0LQXghEwo/n3V\nhyhTN3uZH03bRN3qq9o=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.image.queued", 
    "status": "SUCCESS", 
    "branch": "master", 
    "type": "qcow2", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.image.running

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527246869.0, 
  "msg_id": "2018-655ad819-e64a-4abb-922b-efa282f86bac", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.image.running", 
  "headers": {}, 
  "signature": "KE2H60OojRBaDHPOEtWWi6/fR7BD6aEGGwOOHRiKM8eHkeL1g25ohcPPxbUNtT8pA3nDTn2lFGKj\nKddBYlRCaBD4MLmMaFub1zYNU0pznKdXgeqz4y+CxVgnNpl3CQsviOpFh/SV3g/XuLqmA7aEqpNM\nX7XA9QWLSetxPvCMz14=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.image.running", 
    "status": "SUCCESS", 
    "branch": "master", 
    "type": "''", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.image.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527246953.0, 
  "msg_id": "2018-437ac13b-063b-48e8-94e3-4a8f0ec576d3", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.image.complete", 
  "headers": {}, 
  "signature": "nXeaAMNvfWuYqaOFSTeVs/0SGaH4bb+xYVrmHw2456+vl8OYdW15nvKM8M9zBI9UrtYUbKrLANXP\nYjMsJeOhqpjolU6dZPyUwMfwsTAdYQT6WjtIJjKG/Lz+1k/aRQnd2mpJ4Ebs7sxQzg//XlD5SmnR\nF+nuMBnXX3aRMv+tIDc=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.image.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "type": "qcow2", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.queued

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527247050.0, 
  "msg_id": "2018-3546629a-470e-45db-9e44-32f069190626", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.queued", 
  "headers": {}, 
  "signature": "tfuEb8E/CYaMxyi3o2Jx/4Pu+nWxn6SYKUUwyyp99PkIHSNCQSKvWzo7OkBXAHUUxgmtp2grBhpI\nynRxFTeof+cDpYQcr0cMhYYsDuSaSmzXXtfUcomnmcGxjlbAl7wOuno6uhCM25auO31U7aqITR2H\n7JAVyBGCiSj6mnv9IHg=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.queued", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.running

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527247056.0, 
  "msg_id": "2018-19ffcac4-1d2d-4f9f-9d3e-420c060a01f2", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.running", 
  "headers": {}, 
  "signature": "IYyN/yOakK6fOSWd1AfVhcgKaXgnEIWelLI+/V9I3ZGSrpQZilrG37LeceK/c8Us0cMpmjviFRNI\n8xboxwIYcrG8ZNJho6lU6Oy65QMUrn1hOFSR3ST1YqNzdhPUpoQbmHBN2NJT6VidIxIkOj5AM7R+\nt9TAlTdIKyfmvoJ8SNU=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.running", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527247186.0, 
  "msg_id": "2018-87194aa9-46c2-40be-a18e-aebe3a6907a8", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.complete", 
  "headers": {}, 
  "signature": "kmVrdycbe2lV5geudvMa2NbVv9dxVe7NsNqckfeHzyBe4SOxJJOF7QX4WH/cxCDcGUza32Q7FsRK\nhFIwIZ6SNkJ1aXr7oggWLf/NL3Te+NUMUn7Mfq0nEIbpx42Wcp3gwDFTYUoknbuhc1k22OCpfTta\nk23h0+xLuma8nDh+6FI=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.package.test.functional.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-build.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1527247193.0, 
  "msg_id": "2018-229be71e-a47e-41f8-8432-898bedeaa40e", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-build.complete", 
  "headers": {}, 
  "signature": "YKaxA8lV4ArlVJUWzT57JXMdrke80yswrH/iVO5CArC3Rzu5Z8qMPBCMEMUBaoHxpHp2x22Jg3Nz\nJmz2f61+Ivpm4PtoNJSNLhP0AFYche7ybfAEkLLqbhtcHgR2ioCtl6b1K3HqYqpq1zwnX5xNfZX0\nJIav0gsA2GSyMlwR/XA=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "48", 
    "original_spec_nvr": "initscripts-9.80-1.fc29", 
    "username": "null", 
    "nvr": "initscripts-9.80-1.fc29", 
    "rev": "kojitask-27186311", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-build-pipeline/detail/fedora-rawhide-build-pipeline/48/pipeline/", 
    "namespace": "null", 
    "CI_NAME": "fedora-rawhide-build-pipeline", 
    "repo": "initscripts", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-build.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "",
    "ref": "x86_64",
    "scratch": false
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.package.queued

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894249.0, 
  "msg_id": "2018-8ebfa150-fd80-4db4-8347-f2de679e79d1", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.queued", 
  "headers": {}, 
  "signature": "Q7BX/oQxYXDbhUYuCGPtM5DF1gUNESxkzRB0CrTa62pdT6P/GWG7jNDkj2au2x9cyPd8n1JxgJU4\nnGzA6BbVcCVKCvnRxrtdMgzWrXX5XC/eHNhNVK8u/lm6WQ7mrrNBY8u4leMriXnvjRlS+UFuYRcQ\nwDGx3vs/GqGwCk/Ku3M=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "290", 
    "original_spec_nvr": "", 
    "username": "bgoncalv", 
    "nvr": "", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-pr-pipeline-trigger/detail/fedora-pr-pipeline-trigger/290/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-pr-pipeline-trigger", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.queued", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.package.running

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894293.0, 
  "msg_id": "2018-a4be7e6e-79b8-4af7-9e84-51a3239bf62b", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.running", 
  "headers": {}, 
  "signature": "RSJKEWdJ7mDXKTWt71PJIRuvyLCcoEpTmTajo2XGtZR6LBWSPk/QZ4Zx967Fn4S3d6i6TRjLBzrK\nPPXZgmxH5joA/EXGHaWJFtxmm/IZu7SN7o5vJEcLANVRja5ZIeSriz0gxRepX/9IYxuha3vHmhI5\nSmDb1g8DeXG0PJkjU0U=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "", 
    "username": "bgoncalv", 
    "nvr": "", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.running", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.package.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894467.0, 
  "msg_id": "2018-d47f9710-2bfe-4cda-b01a-b00ab7e30c4a", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.complete", 
  "headers": {}, 
  "signature": "eM6oG8eo4BjrYeY9SQBsw1UeN7H2STuSTz42rMClPXJj2qWx1ejdLaHhOSW8tfQj4pXnp8PZLZjC\nhvgs4BGywT2x8rvcYz4mtbtwdsR9r3fbqwlf1PWSdcL4A78CGzsA6xh2LhvJnTw95l/EonwukFMA\n3Kjv3zyuyEoqgotLgvU=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.image.queued

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894474.0, 
  "msg_id": "2018-f6df45bb-6a0f-4225-aa9d-1914c20bbaa8", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.image.queued", 
  "headers": {}, 
  "signature": "eRdScW32ENLBsUezVBc5Oos2LfbvsdrAU48yr5x4uCNzv3STGsoi3E6w7U0eQdx8/0iCpIh2+9LL\n+tpIWDeZGHgNwbZEuC/la0+7UoAwy6wnMDBqjd9cLMW4y8KiX7b71QQwIeRe6ErpEXbu8Z8HsBuH\nfeBcUE+njZlwxjRLpdg=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.image.queued", 
    "status": "SUCCESS", 
    "branch": "master", 
    "type": "qcow2", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.image.running

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894479.0, 
  "msg_id": "2018-8682fec9-570c-4cb2-b911-b8a2d83f40ad", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.image.running", 
  "headers": {}, 
  "signature": "X6RQjNcB9N4eKfcSIzPqT0/th8XeEu+6Hza4jFd7qayJ5lOwWfUHWLiXNDJPVSBVmFkDb4787Xp0\niKK/UeNGSLYgkCqwk0/ECtHH8IoAdVjiru8LSCMiUqCw20JkDwNx7gtTQvvDvIsMmu5BdUv0Pg8Z\nUoph+8vHBvDrPfPlX+0=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.image.running", 
    "status": "SUCCESS", 
    "branch": "master", 
    "type": "''", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.image.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894573.0, 
  "msg_id": "2018-5b730b1c-026b-47fb-a34c-e6be3b85c4d6", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.image.complete", 
  "headers": {}, 
  "signature": "rGTQHjS7KX89PtXF8wrWHZLEr3zY7hAHDl6zGvhVdRB7da6YVi3f7VBPQtEqQypc4YvwNi++heLY\nRA4FAw85EBZEmMG18pFKJp69RGHebJCYLIcX3dD7T37LEVD2Ggn32HtKoqSVNN/IvgA6TsrC6ibs\nGaPOAn/lJQCALUEjF2k=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.image.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "type": "qcow2", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.queued

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894582.0, 
  "msg_id": "2018-b7a4b79a-f424-41c2-a728-6dcc019772f3", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.queued", 
  "headers": {}, 
  "signature": "emfCdK5+wi2EObsmSAEsX1/E/RfPVDilJry6bOgrumGvVC+Wj3gEP0mBOzXd6yTvk3K7Fve14IR5\nLimdFSqxWZe7LHumv8wmHBiikAXesfTtKxtnpVAEPbEdLLYFjjj2Hj0ZqdobGDXm8xrRxZ4F+Jf2\nBJycfPx/WFbK7C9ljzw=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.queued", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.running

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894589.0, 
  "msg_id": "2018-b6fc8e1a-b036-4609-86c1-9a4534e6cb1b", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.running", 
  "headers": {}, 
  "signature": "KzMLK6P52D3cwwTFAklAhKN/QUIc7YRoWcG/jvhBp5McGsyidVsiUnBTndUljldRd4aIlXqe1O4i\nYX3QJmZenw/QTIY8vcXZQ8NSZBbixPcBAbqNvhmc1ApVHHSgF1eKDStStNrBxkCIF6DgdrA67BIS\nNFjduIf7viDtpddWF4I=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.running", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894708.0, 
  "msg_id": "2018-8bf4b5e7-e900-4e79-a186-392da9a0b241", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.complete", 
  "headers": {}, 
  "signature": "Oq9JiakKPI081rzC7hU3O+cfVbRSDcLjjd8v3ggi6mGsN1MrWrSpy2b6puJ0hnpL3KBN87VkoFPb\nDF3R6e2G8rxCmn5fVmTKTp/MLB9psouFTuX7Dm5swwpogLxmcAosiZ30ujDYmhWCQmvrBemIrBH3\nbxA2rUSDdx+lHamqw6Y=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.package.test.functional.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````

## org.centos.prod.ci.pipeline.allpackages-pr.complete

````
{
  "username": null, 
  "source_name": "datanommer", 
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n", 
  "i": 1, 
  "timestamp": 1526894714.0, 
  "msg_id": "2018-7e09d980-a30d-4ae8-928b-97a2c6af4003", 
  "crypto": "x509", 
  "topic": "org.centos.prod.ci.pipeline.allpackages-pr.complete", 
  "headers": {}, 
  "signature": "tGAagKH+XfKVkb4wwcQkZQmfnl1RjzecpuKbKHRPj3iuwbI3a75Mx1CvDRp6vqSmLmDTWCBwV66H\nyiMpeKka1pzHIxqRUPQZjSNj5zZz60iByMMtDMeTP5xMEy3WOEmhnsxhUuXd/RFp85HKbp/8nG11\nOT1I7ulvX2u7g6Z3xfw=\n", 
  "source_version": "0.9.0", 
  "msg": {
    "CI_TYPE": "custom", 
    "build_id": "20", 
    "original_spec_nvr": "calc-2.12.6.7-2.fc29", 
    "username": "bgoncalv", 
    "nvr": "calc-2.12.6.7-2.fc29.pr.ef8cadbd16624467a0c979d89abfa20e", 
    "rev": "PR-1", 
    "message-content": "", 
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/fedora-rawhide-pr-pipeline/detail/fedora-rawhide-pr-pipeline/20/pipeline/", 
    "namespace": "rpms", 
    "CI_NAME": "fedora-rawhide-pr-pipeline", 
    "repo": "calc", 
    "topic": "org.centos.prod.ci.pipeline.allpackages-pr.complete", 
    "status": "SUCCESS", 
    "branch": "master", 
    "test_guidance": "''", 
    "comment_id": "1234",
    "ref": "x86_64",
    "scratch": true
  }
}
````
