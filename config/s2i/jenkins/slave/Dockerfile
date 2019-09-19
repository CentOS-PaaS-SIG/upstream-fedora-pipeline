FROM openshift/jenkins-slave-base-centos7:v3.6
##
## ------------------------------------->  ^^ this is needed
## since the centosCI openshift cluster
## is running 3.6 and the slave needs the
## correct 'oc' binary to work properly
## This should be updated when the cluster
## is upgraded.
##

RUN yum install -y epel-release
# add ruby for ghi
# add yum-utils for yumdownloader
RUN yum install -y ansible \
        jq \
        ruby \
        yum-utils
# Install STR to slave to be able to run checkTests using ansible
RUN yumdownloader standard-test-roles
RUN rpm -ivh --nodeps standard-test-roles*.rpm
