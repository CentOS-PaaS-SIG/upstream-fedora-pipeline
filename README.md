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
  - [org.centos.prod.allpackages.pipeline.package.queued](#orgcentosprodallpackagespipelinepackagequeued)
  - [org.centos.prod.allpackages.pipeline.package.running](#orgcentosprodallpackagespipelinepackagerunning)
  - [org.centos.prod.allpackages.pipeline.package.complete](#orgcentosprodallpackagespipelinepackagecomplete)
  - [org.centos.prod.allpackages.pipeline.image.queued](#orgcentosprodallpackagespipelineimagequeued)
  - [org.centos.prod.allpackages.pipeline.image.running](#orgcentosprodallpackagespipelineimagerunning)
  - [org.centos.prod.allpackages.pipeline.image.complete](#orgcentosprodallpackagespipelineimagecomplete)
  - [org.centos.prod.allpackages.pipeline.image.test.smoke.queued](#orgcentosprodallpackagespipelineimagetestsmokequeued)
  - [org.centos.prod.allpackages.pipeline.image.test.smoke.running](#orgcentosprodallpackagespipelineimagetestsmokerunning)
  - [org.centos.prod.allpackages.pipeline.image.test.smoke.complete](#orgcentosprodallpackagespipelineimagetestsmokecomplete)
  - [org.centos.prod.allpackages.pipeline.package.test.functional.queued](#orgcentosprodallpackagespipelinepackagetestfunctionalqueued)
  - [org.centos.prod.allpackages.pipeline.package.test.functional.running](#orgcentosprodallpackagespipelinepackagetestfunctionalrunning)
  - [org.centos.prod.allpackages.pipeline.package.test.functional.complete](#orgcentosprodallpackagespipelinepackagetestfunctionalcomplete)
  - [org.centos.prod.allpackages.pipeline.complete](#orgcentosprodallpackgespipelinecomplete)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# All Packages Fedora Pipeline Overview

The All Packages Fedora Pipeline serves to test all commits to dist-git for Fedora packages. The pipeline watches all branches corresponding to major Fedora releases and Rawhide. The pipeline begins by submitting a koji scratch build for the new change. Once that build is finished, the pipeline uses imagefactory to construct a cloud qcow2 image. The pipeline adds the newly created rpm at build time to the qcow2 image. Once the image generation is complete, the pipeline runs any tests that are included in the package's dist-git repo, as defined by the standard-test-roles. All stages of the pipeline send out messages on fedmsg for their status.<br><br>

# All Packages Fedora Pipeline Stages

## Trigger

Once packages are pushed to Fedora dist-git this will trigger a message.  The pipeline will be triggered via the [Jenkins JMS plugin](https://wiki.jenkins-ci.org/display/JENKINS/JMS+Messaging+Plugin) for dist-git messages on fedmsg.  
Only changes pushed to a fXX or master branch in dist-git are monitored.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.allpackages.pipeline.package.[queued,ignored].

## Build Package

The pipeline job begins by submitting a new scratch build to koji for the rpm. Once the koji build is complete, the artifacts, including logs, are downloaded to the Jenkins workspace to be used by the pipeline build and stored as artifacts in Jenkins.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.allpackages.pipeline.package.[running,complete].

## Compose cloud qcow2 image

The pipeline next uses imagefactory to build a cloud qcow2 image. The image will have its kickstart modified to include the newly built rpm at build time.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.allpackages.pipeline.image.[queued,running,complete].

## Image Smoke Test Validation

This validation ensures that the newly created qcow2 image can boot. It also checks that the rpm created in the previous stage is successfully installed on the host.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.allpackages.pipeline.image.test.smoke.[queued,running,complete].

## Functional Tests on Packages

Functional tests will be executed on the produced package from the previous stage of the pipeline if they exist.  This will help identify issues isolated to the package themselves.  Success or failure will result with a fedmsg back to the Fedora package maintainer. The tests are pulled from the dist-git repos and are executed with the standard-test-roles. If no tests exist, this stage is skipped.

Pipeline messages sent via fedmsg for this stage are captured by the topics org.centos.prod.allpackages.pipeline.package.test.functional.[queued,running,complete].

# fedmsg Bus

Communication between Fedora, CentOS, and Red Hat infrastructures will be done via fedmsg.  Messages will be received of updates to Fedora dist-git repos.  Triggering will happen from Fedora dist-git. The pipeline in CentOS infrastructure will build packages, compose a cloud qcow2 image, and run the standard test roles standard tests from the package's dist-git.  We are dependant on CentOS Infrastructure for allowing us a hub for publishing messages to fedmsg.

## fedmsg - Message Types
Below are the different message types that we listen and publish.  There will be different subtopics so we can keep things organized under the org.centos.prod.allpackages.pipeline.* umbrella. The fact that ‘org.centos’ is contained in the messages is a side effect of the way fedmsg enforces message naming.

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
  - ex. org.centos.prod.allpackages.pipeline.image.complete
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

## org.centos.prod.allpackages.pipeline.package.queued

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.package.running

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.package.complete

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.image.queued

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.image.running

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.image.complete

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.image.test.smoke.queued

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.image.test.smoke.running

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.image.test.smoke.complete

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.package.test.functional.queued

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.package.test.functional.running

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.package.test.functional.complete

````
TODO once we have some already sent
````

## org.centos.prod.allpackages.pipeline.complete

````
TODO once we have some already sent
````
