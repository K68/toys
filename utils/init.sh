#!/bin/bash
uname -r
apt-get update
apt-get install -y apt-transport-https ca-certificates
apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
file="/etc/apt/sources.list.d/docker.list"
echo "deb https://apt.dockerproject.org/repo ubuntu-xenial main" >> $file
apt-get update
apt-get purge lxc-docker
apt-cache policy docker-engine
apt-get install -y linux-image-extra-$(uname -r) linux-image-extra-virtual
apt-get update
apt-get install -y docker-engine
apt-get install -y aufs-tools cgroupfs-mount cgroup-lite
curl -sSL https://github.com/ToyYang/toys/raw/scripts/utils/local.conf > /etc/sysctl.d/local.conf
sysctl --system
service docker start
apt-get install -y p7zip-full lrzsz nginx
apt-get install -y software-properties-common
add-apt-repository ppa:webupd8team/java
apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 seen true" | debconf-set-selections
apt-get install -y oracle-java8-installer
apt install oracle-java8-set-default
echo "JAVA_HOME="/usr/lib/jvm/java-8-oracle"" >> /etc/environment
source /etc/environment
echo $JAVA_HOME
apt-get install -y python-pip
pip install setuptools
