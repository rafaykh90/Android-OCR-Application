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

# Log in to Google Cloud
gcloud auth login

# Build backend Docker image
sudo docker build -t $PROJECT_ID/$DOCKER_IMAGE_NAME backend

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

# Create cluster
gcloud container --project $PROJECT_ID clusters create $CLUSTER_NAME --zone $ZONE --num-nodes 2 --network default

# Create volumes for the database containers/nodes
gcloud compute disks create --project $PROJECT_ID --zone $ZONE --size 200GB mongo-persistent-storage-node-1-disk
gcloud compute disks create --project $PROJECT_ID --zone $ZONE --size 200GB mongo-persistent-storage-node-2-disk
gcloud compute disks create --project $PROJECT_ID --zone $ZONE --size 200GB mongo-persistent-storage-node-3-disk

# Get Application Default Credentials
gcloud auth application-default login

# Select the correct cluster for kubectl (in case cluster already exist)
gcloud container clusters get-credentials --project $PROJECT_ID --zone $ZONE $CLUSTER_NAME

# Create Kubernetes replication controllers for the database replica set
sed -e 's~<num>~1~g' backend/mongo-controller-template.yaml > /tmp/mongo-controller-1.yaml | kubectl create -f /tmp/mongo-controller-1.yaml
sed -e 's~<num>~2~g' backend/mongo-controller-template.yaml > /tmp/mongo-controller-2.yaml | kubectl create -f /tmp/mongo-controller-2.yaml
sed -e 's~<num>~3~g' backend/mongo-controller-template.yaml > /tmp/mongo-controller-3.yaml | kubectl create -f /tmp/mongo-controller-3.yaml

# Create a Kubernetes service for the database containers/nodes
sed -e 's~<num>~1~g' backend/mongo-service-template.yaml > /tmp/mongo-service-1.yaml | kubectl create -f /tmp/mongo-service-1.yaml
sed -e 's~<num>~2~g' backend/mongo-service-template.yaml > /tmp/mongo-service-2.yaml | kubectl create -f /tmp/mongo-service-2.yaml
sed -e 's~<num>~3~g' backend/mongo-service-template.yaml > /tmp/mongo-service-3.yaml | kubectl create -f /tmp/mongo-service-3.yaml

# Create a Kubernetes replication controller and service for the backend containers/nodes
kubectl create -f backend/web-controller.yml
kubectl create -f backend/web-service.yml

# Describe the web service to show the external IP
echo 
echo "Sleeping for 90 seconds to allow the web load-balancer to finish loading."
echo "Use the following command to get the EXTERNAL-IP of the backend if it has not finish by then:"
echo "    kubectl get services web"
sleep 90
echo 
kubectl get services web
