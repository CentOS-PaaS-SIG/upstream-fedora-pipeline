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
  - [org.centos.prod.ci.pipeline.allpackages.package.queued](#orgcentosprodcipipelineallpackagespackagequeued)
  - [org.centos.prod.ci.pipeline.allpackages.package.running](#orgcentosprodcipipelineallpackagespackagerunning)
  - [org.centos.prod.ci.pipeline.allpackages.package.complete](#orgcentosprodcipipelineallpackagespackagecomplete)
  - [org.centos.prod.ci.pipeline.allpackages.image.queued](#orgcentosprodcipipelineallpackagesimagequeued)
  - [org.centos.prod.ci.pipeline.allpackages.image.running](#orgcentosprodcipipelineallpackagesimagerunning)
  - [org.centos.prod.ci.pipeline.allpackages.image.complete](#orgcentosprodcipipelineallpackagesimagecomplete)
  - [org.centos.prod.ci.pipeline.allpackages.image.test.smoke.queued](#orgcentosprodcipipelineallpackagesimagetestsmokequeued)
  - [org.centos.prod.ci.pipeline.allpackages.image.test.smoke.running](#orgcentosprodcipipelineallpackagesimagetestsmokerunning)
  - [org.centos.prod.ci.pipeline.allpackages.image.test.smoke.complete](#orgcentosprodcipipelineallpackagesimagetestsmokecomplete)
  - [org.centos.prod.ci.pipeline.allpackages.package.test.functional.queued](#orgcentosprodcipipelineallpackagespackagetestfunctionalqueued)
  - [org.centos.prod.ci.pipeline.allpackages.package.test.functional.running](#orgcentosprodcipipelineallpackagespackagetestfunctionalrunning)
  - [org.centos.prod.ci.pipeline.allpackages.package.test.functional.complete](#orgcentosprodcipipelineallpackagespackagetestfunctionalcomplete)
  - [org.centos.prod.ci.pipeline.allpackages.complete](#orgcentosprodcipipelineallpackagescomplete)

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

The pipeline next uses imagefactory to build a cloud qcow2 image. The image will have its kickstart modified to include the newly built rpm at build time.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.image.[queued,running,complete].

## Image Smoke Test Validation

This validation ensures that the newly created qcow2 image can boot. It also checks that the rpm created in the previous stage is successfully installed on the host.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.image.test.smoke.[queued,running,complete].

## Functional Tests on Packages

Functional tests will be executed on the produced package from the previous stage of the pipeline if they exist.  This will help identify issues isolated to the package themselves.  Success or failure will result with a fedmsg back to the Fedora package maintainer. The tests are pulled from the dist-git repos and are executed with the standard-test-roles. If no tests exist, this stage is skipped.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.ci.pipeline.allpackages.package.test.functional.[queued,running,complete].

# fedmsg Bus

Communication between Fedora, CentOS, and Red Hat infrastructures will be done via fedmsg.  Messages will be received of updates to Fedora dist-git repos.  Triggering will happen from Fedora dist-git. The pipeline in CentOS infrastructure will build packages, compose a cloud qcow2 image, and run the standard test roles standard tests from the package's dist-git.  We are dependant on CentOS Infrastructure for allowing us a hub for publishing messages to fedmsg.

## fedmsg - Message Types
Below are the different message types that we listen and publish.  There will be different subtopics so we can keep things organized under the org.centos.prod.ci.pipeline.allpackages.* umbrella. The fact that ‘org.centos’ is contained in the messages is a side effect of the way fedmsg enforces message naming.

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
* ref - Indication of what we are building distro/branch/arch/distro_type
  - ex. x86_64
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

## org.centos.prod.ci.pipeline.allpackages.package.queued

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522748480.0,
  "msg_id": "2018-02592ed2-c36a-4370-9f12-8c5448ad20b7",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.package.queued",
  "headers": {},
  "signature": "1oXkyEA0n3UEsMJ4XscHJJjolBwVzeWk7RaC9IRGceVrX6vjVgTsRqmAWrsty0zO87jIhs5jrRFD\nOmrJWSvPFJ2+6h536ws1zngaztOP31z/jetFd4opoLUIGO7pOAxLg9/LF3U1bK/HCUZrwaNM/xou\ncq+Tg1kb3Gh6jggOVg8=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "6917",
    "original_spec_nvr": "",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-pipeline-trigger/detail/upstream-fedora-pipeline-trigger/6917/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-pipeline-trigger",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.package.queued",
    "status": "SUCCESS",
    "branch": "f28",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.package.running

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522759206.0,
  "msg_id": "2018-bcabe210-5c19-43d0-890e-eee577a7adc0",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.package.running",
  "headers": {},
  "signature": "mJe9EhAsuKWddCaYEVW/aO3mHK0eb02hupMoPIeu1hqOnW+Ky9vvDQKi4SD5xEwb1dYvCoOrWlP4\n40uShK+4h1+GYbLdqrr3MVmMFFJgkvt6/nbb0OIswU9IaEyE9wdDv6u0uveY5AydngG4oh42sMay\nGWNkFoKEYW7r5dkU/HU=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.package.running",
    "status": "SUCCESS",
    "branch": "f28",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.package.complete

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760188.0,
  "msg_id": "2018-b49a2e04-2acb-4220-804c-eda60b322f56",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.package.complete",
  "headers": {},
  "signature": "eRz1PqPKSTSB2IiFKCzvQe38s3OHOjVusNqIq+mRFyNNIH3MQvaAufL3K3hyJ8wWSdxtnMEHSWAM\nsblJha1XyYbGNxW33Rd7HygN6XcuWZm6ZBC3dnM845MbAG2eIOgpqCI9qvULWA/J8to1STbHPv3a\n3godUxG2XSjaAqQlZMQ=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.package.complete",
    "status": "SUCCESS",
    "branch": "f28",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.image.queued

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760194.0,
  "msg_id": "2018-6f7a8efe-53b5-4292-8f05-40f71c74d358",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.image.queued",
  "headers": {},
  "signature": "y6/ffq2zKawe311BPWylUlCsOXamglEZp2BrnrEG1ziPC8qOKBDnB6OWp62vJ7+udIg4Tbc4yFjU\nFgJLPPCqMFXxlD20PwgWo1oO0Ic+mrnzqFkHptLM5B+VqSWN/L5qhrMYhY8RMk3akD8bPm350fYZ\n+N+qO18SRw0UmJ5GrFA=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.image.queued",
    "status": "SUCCESS",
    "branch": "f28",
    "type": "qcow2",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.image.running

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760200.0,
  "msg_id": "2018-869aac7b-08b3-4a6e-ad51-36c0a5e879c7",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.image.running",
  "headers": {},
  "signature": "FU7A4krrYbHHoQzLDFqDB+fW9C95rAZppvCJx3/sn/QkHreJcZO93+6Y99NVuy2BnFAXQZubOOCK\nvHxgSuOj8cy8YdWugmD2Q7Q5ygzPeXJa42r45trAAJAMR5pX4AjBlvnS0cBXca4iZEK42TLXzVrY\n3ViJ1x08RRLiHVA6YGE=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.image.running",
    "status": "SUCCESS",
    "branch": "f28",
    "type": "''",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.image.complete

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760784.0,
  "msg_id": "2018-2344dbc5-a102-4428-b31e-b8ede50412a2",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.image.complete",
  "headers": {},
  "signature": "ZdIskYmNfnxp8dt6biaoPdvtOUsxKmcq6Z2FKPGZ1D1npJKuhm4e2F6rIZvRxgDm3wcKqNOzHsQN\nnwMC0aYKZM8q5Z/07vMxP7XTuFGC7TyLyxQPWaxzvcgZHvNrZqgDO/skFh4IyNbX1lKfCY4BiGXb\nA0J8baZJJCSMgjtlF1M=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.image.complete",
    "status": "SUCCESS",
    "branch": "f28",
    "type": "qcow2",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.image.test.smoke.queued

````
TODO once we have some already sent
````

## org.centos.prod.ci.pipeline.allpackages.image.test.smoke.running

````
TODO once we have some already sent
````

## org.centos.prod.ci.pipeline.allpackages.image.test.smoke.complete

````
TODO once we have some already sent
````

## org.centos.prod.ci.pipeline.allpackages.package.test.functional.queued

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760790.0,
  "msg_id": "2018-1cb16c54-cf1e-4cfe-874f-bdb1b89a9042",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.package.test.functional.queued",
  "headers": {},
  "signature": "YBK5V3wSp0V0qMNOzhqmy5ZysdPeW6zNHn0xRf7nXo7DBH3W10+38e5znbZVe7Yn96K2xbvlfB1M\nqp5Hm9isQsQQK6eoCDFoVpj/dP3BGy+/kZsw1XqRGXQJ9yq/dVWtwQLWa3xCEQvnifjf2Mwc2Ud1\nIG8wmbhnDclCZ5vji8Q=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.package.test.functional.queued",
    "status": "SUCCESS",
    "branch": "f28",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.package.test.functional.running

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760797.0,
  "msg_id": "2018-ab27af60-1967-42e2-8f10-524ee14193b2",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.package.test.functional.running",
  "headers": {},
  "signature": "FZ9aglc4Y3DU82GqYhkB5b8HweFoOPZajT7X/Qnp2VkWJpqazZStnJ6AeYYA9SCHI7Nw8YTW9ppW\nO/vzWID43cH2b07Ys5vQSDOQM1BgY2hDSVhw7KIsLPGHKM+7dE9BGBYQqk1ok/0wJ6mGrSf3JhZt\nwJjyq3VO9X/uYAQrxAU=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.package.test.functional.running",
    "status": "SUCCESS",
    "branch": "f28",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````

## org.centos.prod.ci.pipeline.allpackages.package.test.functional.complete

````
TODO once we have some already sent
````

## org.centos.prod.ci.pipeline.allpackages.complete

````
{
  "username": null,
  "source_name": "datanommer",
  "certificate": "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tCk1JSUVPakNDQTZPZ0F3SUJBZ0lDQW5Fd0RRWUpL\nb1pJaHZjTkFRRUZCUUF3Z2FBeEN6QUpCZ05WQkFZVEFsVlQKTVFzd0NRWURWUVFJRXdKT1F6RVFN\nQTRHQTFVRUJ4TUhVbUZzWldsbmFERVhNQlVHQTFVRUNoTU9SbVZrYjNKaApJRkJ5YjJwbFkzUXhE\nekFOQmdOVkJBc1RCbVpsWkcxelp6RVBNQTBHQTFVRUF4TUdabVZrYlhObk1ROHdEUVlEClZRUXBF\nd1ptWldSdGMyY3hKakFrQmdrcWhraUc5dzBCQ1FFV0YyRmtiV2x1UUdabFpHOXlZWEJ5YjJwbFkz\nUXUKYjNKbk1CNFhEVEUzTURVeE1ERTBNamMwT0ZvWERUSTNNRFV3T0RFME1qYzBPRm93Z2NneEN6\nQUpCZ05WQkFZVApBbFZUTVFzd0NRWURWUVFJRXdKT1F6RVFNQTRHQTFVRUJ4TUhVbUZzWldsbmFE\nRVhNQlVHQTFVRUNoTU9SbVZrCmIzSmhJRkJ5YjJwbFkzUXhEekFOQmdOVkJBc1RCbVpsWkcxelp6\nRWpNQ0VHQTFVRUF4TWFabVZrYlhObkxYSmwKYkdGNUxtTnBMbU5sYm5SdmN5NXZjbWN4SXpBaEJn\nTlZCQ2tUR21abFpHMXpaeTF5Wld4aGVTNWphUzVqWlc1MApiM011YjNKbk1TWXdKQVlKS29aSWh2\nY05BUWtCRmhkaFpHMXBia0JtWldSdmNtRndjbTlxWldOMExtOXlaekNCCm56QU5CZ2txaGtpRzl3\nMEJBUUVGQUFPQmpRQXdnWWtDZ1lFQTJzYUJuSjNyTlhYQXV3Skt2UkJyQnJTYUdMWHgKYXg4VGhu\nZ0wxV2hCYS8wSFZVdVAxWEhWUEVweUh6YXZZK0dsRzFVclVUMkFMQzFuRk5nVUNpSjhWWWVoZElw\nWApzQzNiOHFnUmltekt0aHUxM2hqQ01kSTYzV3h1S3FBQk5UQTRkZWtBK1c2cE9EdVdIMEI1b0tq\nVjFmWkZRN2xFCjUzZlQybElBZWg4ZndZY0NBd0VBQWFPQ0FWY3dnZ0ZUTUFrR0ExVWRFd1FDTUFB\nd0xRWUpZSVpJQVliNFFnRU4KQkNBV0hrVmhjM2t0VWxOQklFZGxibVZ5WVhSbFpDQkRaWEowYVda\ncFkyRjBaVEFkQmdOVkhRNEVGZ1FVUytnVApwNmg2ZXpJZW5RK0lLUERnWmZWZHQ5a3dnZFVHQTFV\nZEl3U0J6VENCeW9BVWEwQmErUklJaVZubldlVUY5UUlkCkNrNS9GQUNoZ2Fha2dhTXdnYUF4Q3pB\nSkJnTlZCQVlUQWxWVE1Rc3dDUVlEVlFRSUV3Sk9RekVRTUE0R0ExVUUKQnhNSFVtRnNaV2xuYURF\nWE1CVUdBMVVFQ2hNT1JtVmtiM0poSUZCeWIycGxZM1F4RHpBTkJnTlZCQXNUQm1abApaRzF6WnpF\nUE1BMEdBMVVFQXhNR1ptVmtiWE5uTVE4d0RRWURWUVFwRXdabVpXUnRjMmN4SmpBa0Jna3Foa2lH\nCjl3MEJDUUVXRjJGa2JXbHVRR1psWkc5eVlYQnliMnBsWTNRdWIzSm5nZ2tBNDFBZVIwOFhIa1V3\nRXdZRFZSMGwKQkF3d0NnWUlLd1lCQlFVSEF3SXdDd1lEVlIwUEJBUURBZ2VBTUEwR0NTcUdTSWIz\nRFFFQkJRVUFBNEdCQUF5cApCUk43VXFaUU1vcUw3UkFnS09hMzFSVTh3R3lWaEJhd1NvZm1Qd1dT\nMUdEbVA1OU9FbElaRldrVisrTi92VXBSCmFjalFyTStoUEVEYXRaUVU5cEtiV3FmVy92WVVyaGpE\nYTNYV3dxeW1kT2hjWTFhWUR3aVE5NGlWekNGUkdFM2kKMXNkN2tuc2VjL2x4Z2NldmhYS2ZleTNK\nN241cXludFBYVGpVMjdGMQotLS0tLUVORCBDRVJUSUZJQ0FURS0tLS0tCg==\n",
  "i": 1,
  "timestamp": 1522760985.0,
  "msg_id": "2018-201fffd3-28ad-45f4-91d3-a4ec68a73691",
  "crypto": "x509",
  "topic": "org.centos.prod.allpackages.pipeline.complete",
  "headers": {},
  "signature": "ebDcnGyxbRADyR5YRppF6rE63tYLqkuRcYzhaXo2BsOls5cLoLwdU+jm1/C/wx5WoEGJOm6EPAkF\nJRNjdcItocEHQymJCvxML/tWmkHwInEew+qNRcx35kCc3d3+7ATknhoj2gnWmHKd139GzhKrX0o3\nNRQRa6y9ktyYcVGCFAA=\n",
  "source_version": "0.8.2",
  "msg": {
    "CI_TYPE": "custom",
    "build_id": "369",
    "original_spec_nvr": "bind-9.11.3-5.fc28",
    "username": "pemensik",
    "nvr": "",
    "rev": "36ff6aebe60cea38d08c36b8c8494f62f593ef1a",
    "message-content": "",
    "build_url": "https://jenkins-continuous-infra.apps.ci.centos.org/blue/organizations/jenkins/upstream-fedora-f28-pipeline/detail/upstream-fedora-f28-pipeline/369/pipeline/",
    "namespace": "rpms",
    "CI_NAME": "upstream-fedora-f28-pipeline",
    "repo": "bind",
    "topic": "org.centos.prod.ci.pipeline.allpackages.complete",
    "status": "SUCCESS",
    "branch": "f28",
    "test_guidance": "''",
    "ref": "x86_64"
  }
}
````
