# APIs
----
1. User signup: POST /signuplocal (Local Authentication)
2. User log-in: POST /loginlocal (Local Authentication)
3. History of user: POST /history
4. Upload images: POST /ocr
5. Get a specific image: GET /image/:imgname
6. Logout: GET /logout

For detailed implementation, please go to 'backend/app/routes/index.js'.

## POST Request Body Parameters (JSON)
1. For user sign-up using local authentication:
  *username: String,
  *password: String,
2. For user log-in using local authentication:
  *username: String,
  *password: String
4. For uploading images:
	Images to be uploaded with form-data body. Do not set headers.

(* required parameters)

## Request Route Parameters
1. For fetching an image:
  /image/:imgname  (imgname = name of desired image)

## Api /image/:imgname return image in following format:
  ```
  {
    message: base64 representation of image
  }
  ```

## API /history and /ocr return JSON in following format:
  ```
  {
    message: {
      username: String,
      createdAt: Date,
      imgText: Array,
      imgNames: Array,
      imgThumbs: Array containg base64 representation of thumbnails
    }
  }
  ```

### API /ocr additionally returns the following JSON:
  ```
  {
    message: {...},
    imageStatistics: {
      maxTime: Number,
      indexOfMax: Number,
      minTime: Number,
      indexOfMin: Number,
      average: Number,
      stdev: Number,
      ocrTimesInSeconds: Array of Numbers,
      imageSizesInBytes: Array of Numbers
    }
  }
  ```

## Development Instructions
----
### Prerequisites

See Backend Installation section

### How to deploy backend codes to the existing Kubernetes cluster

1. Login to Google Cloud (if haven't previously or executed deploy.sh)

    ```
    $ gcloud auth login
    $ gcloud auth application-default login
    ```

2. Run dev-update-backend.sh

    ```
    $ cd backend
    $ ./dev-update-backend.sh
    ```

### How to get backend public/external IP

Check the *EXTERNAL-IP* value of the *web* service via the *get* command of Kubernetes client. E.g.:

    $ kubectl get services web
    NAME         CLUSTER-IP      EXTERNAL-IP      PORT(S)     AGE
    web          10.59.251.54    104.155.20.226   443/TCP     1m

### How to check the logs

1. Use the *get* command of Kubernetes client to get the name of the of pods. E.g.:

    ```
    $ kubectl get pods
    NAME                   READY     STATUS    RESTARTS   AGE
    mongo-1-ptpbz          2/2       Running   0          10h
    mongo-2-d2qn4          2/2       Running   0          9h
    mongo-3-btxrx          2/2       Running   0          10h
    web-controller-3q60k   1/1       Running   0          2m
    web-controller-xn525   1/1       Running   0          2m
    ```

2. Use the *logs* command of Kubernetes client to display the output. E.g.:

    ```
    $ kubectl logs web-controller-3q60k
    Database connection established.
    ```
