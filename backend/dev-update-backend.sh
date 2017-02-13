#!/bin/bash

#
# Script to deploy backend code to an existing Kubernetes cluster in Google Container Engine
#  
# Pre-requisites:
#   Docker installed                - run ../install_tools.sh
#   Google Cloud SDK installed      - run ../install_tools.sh
#   Loged-in to Google Cloud        - run gcloud auth login
#   Application Default Credentials - run gcloud auth application-default login
#

PROJECT_ID="mcc-2016-g05-p2"
ZONE="europe-west1-c"
DOCKER_IMAGE_NAME="backend"
CLUSTER_NAME="mcc-2016-g05-p2"

# Build backend Docker image
sudo docker build -t $PROJECT_ID/$DOCKER_IMAGE_NAME .

# Push backend Docker image to Google Cloud Container Registry
sudo docker tag $PROJECT_ID/$DOCKER_IMAGE_NAME gcr.io/$PROJECT_ID/$DOCKER_IMAGE_NAME
sudo `which gcloud` docker push gcr.io/$PROJECT_ID/$DOCKER_IMAGE_NAME

# Install Kubernetes client
if [ ! -f /usr/local/bin/kubectl ]; then
    curl https://storage.googleapis.com/kubernetes-release/release/v1.4.6/kubernetes-client-linux-amd64.tar.gz > /tmp/kubernetes-client-linux-amd64.tar.gz
    tar -xzf /tmp/kubernetes-client-linux-amd64.tar.gz -C /tmp/
    sudo mv /tmp/kubernetes/client/bin/kubectl /usr/local/bin/kubectl
    sudo chmod +x /usr/local/bin/kubectl
fi

# Select the correct cluster for kubectl
gcloud container clusters get-credentials --project $PROJECT_ID --zone $ZONE $CLUSTER_NAME

# Delete the existing Kubernetes replication controller of the backend containers
kubectl delete rc web-controller

# Recreate a Kubernetes replication controller for the backend containers
kubectl create -f web-controller.yml
