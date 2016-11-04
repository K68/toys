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
