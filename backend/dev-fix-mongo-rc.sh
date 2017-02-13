#!/bin/bash

#
# Create a Kubernetes cluster in Google Container Engine and deploy backend app
#  
# Pre-requisites:
#   Docker installed           - run ../install_tools.sh
#   Google Cloud SDK installed - run ../install_tools.sh
#

PROJECT_ID="mcc-2016-g05-p2"
ZONE="europe-west1-c"
DOCKER_IMAGE_NAME="backend"
CLUSTER_NAME="mcc-2016-g05-p2"

# Install Kubernetes client
if [ ! -f /usr/local/bin/kubectl ]; then
    curl https://storage.googleapis.com/kubernetes-release/release/v1.4.6/kubernetes-client-linux-amd64.tar.gz > /tmp/kubernetes-client-linux-amd64.tar.gz
    tar -xzf /tmp/kubernetes-client-linux-amd64.tar.gz -C /tmp/
    sudo mv /tmp/kubernetes/client/bin/kubectl /usr/local/bin/kubectl
    sudo chmod +x /usr/local/bin/kubectl
fi

# Select the correct cluster for kubectl (in case cluster already exist)
gcloud container clusters get-credentials --project $PROJECT_ID --zone $ZONE $CLUSTER_NAME

# Delete existing replication controllers for the database replica set
kubectl delete rc mongo-1
kubectl delete rc mongo-2
kubectl delete rc mongo-3

echo Sleeping for 1 min to allow pods to shutdown
sleep 60

# Delete existing (broken) volumes of the database containers
gcloud compute disks delete --project $PROJECT_ID --zone $ZONE mongo-persistent-storage-node-1-disk
gcloud compute disks delete --project $PROJECT_ID --zone $ZONE mongo-persistent-storage-node-2-disk
gcloud compute disks delete --project $PROJECT_ID --zone $ZONE mongo-persistent-storage-node-3-disk

# Re-create volumes for the database containers
gcloud compute disks create --project $PROJECT_ID --zone $ZONE --size 200GB mongo-persistent-storage-node-1-disk
gcloud compute disks create --project $PROJECT_ID --zone $ZONE --size 200GB mongo-persistent-storage-node-2-disk
gcloud compute disks create --project $PROJECT_ID --zone $ZONE --size 200GB mongo-persistent-storage-node-3-disk

# Re-create Kubernetes replication controllers for the database replica set
sed -e 's~<num>~1~g' mongo-controller-template.yaml > /tmp/mongo-controller-1.yaml | kubectl create -f /tmp/mongo-controller-1.yaml
sed -e 's~<num>~2~g' mongo-controller-template.yaml > /tmp/mongo-controller-2.yaml | kubectl create -f /tmp/mongo-controller-2.yaml
sed -e 's~<num>~3~g' mongo-controller-template.yaml > /tmp/mongo-controller-3.yaml | kubectl create -f /tmp/mongo-controller-3.yaml

