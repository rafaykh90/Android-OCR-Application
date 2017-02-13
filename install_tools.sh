#!/bin/bash

# Install Docker
sudo apt-get update
sudo apt-get -y install apt-transport-https ca-certificates
sudo apt-key adv --keyserver hkp://ha.pool.sks-keyservers.net:80 --recv-keys 58118E89F3A912897C070ADBF76221572C52609D
echo deb https://apt.dockerproject.org/repo ubuntu-xenial main | sudo tee /etc/apt/sources.list.d/docker.list
sudo apt-get update
sudo apt-get -y install linux-image-extra-$(uname -r) linux-image-extra-virtual
sudo apt-get -y install docker-engine
sudo service docker start

# Install Kubernetes client
if [ ! -f /usr/local/bin/kubectl ]; then
    curl https://storage.googleapis.com/kubernetes-release/release/v1.4.6/kubernetes-client-linux-amd64.tar.gz > /tmp/kubernetes-client-linux-amd64.tar.gz
    tar -xzf /tmp/kubernetes-client-linux-amd64.tar.gz -C /tmp/
    sudo mv /tmp/kubernetes/client/bin/kubectl /usr/local/bin/kubectl
    sudo chmod +x /usr/local/bin/kubectl
fi

# Install Google Cloud SDK
export CLOUDSDK_CORE_DISABLE_PROMPTS=1
curl https://dl.google.com/dl/cloudsdk/release/install_google_cloud_sdk.bash | bash

