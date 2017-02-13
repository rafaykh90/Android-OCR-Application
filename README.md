## Repository Structure
----

* **./backend/** - contains development scripts, and Kubernetes and Docker configuration files
* **./backend/app** - contains backend app source code
* **./frontend/android** - contains the Android client source code
* **./frontend/external/** - contains sources of external Android client dependencies


## Back-end App Directory Structure
----

backend/app/bin/www
    * Creates and starts the https web server

backend/app/models/userSchema.js
    * Contains the mongoose schema and model definition for user realted data used in local authentication.

backend/app/models/imageSchema.js
    * Contains the mongoose schema and model definition for storing images. Expiry of image records is set by reading 'SOURCE_IMAGE_LIFETIME' environment variable value here.

backend/app/models/imageDataSchema.js
    * Contains the mongoose schema and model definition for image related data and thumbnails.        

backend/app/passportSetup
    * Contains files for passport middleware implementation which are used for local authentication.

backend/app/routes/index.js
    * Handles API calls to the backend.

backend/app/routes/ocr.js
    * Handles thumbnail creation and OCR.

backend/app/db.js
    * Connects backend to the mogodb replica set.

backend/app/app.js
    * Creates various variables and ties them to used packages, node functionality, routes and implement error handling.



## Backend Installation
----
### Prerequisites

*Ubuntu 16.04* with the following installed:

1. Docker
2. Google Cloud SDK
3. Kubernetes client

Tools can be installed by running the following script:

    $ ./install_tools.sh

### Instructions

* Create a Kubernetes cluster in Google Container Engine and deploy backend app by running the following script:

    ```
    $ ./deploy.sh
    ```

## Frontend Installation
----
### Prerequisites

* Recent JDK in JAVA_HOME or in path
* Recent Android SDK in ANDROID_HOME, with license agreements accepted:
    - SDK build tools 22.0.1
    - SDK Platform 22
    - Android Support Repository
    - Google Repository

To install prerequisites, run:

```
mkdir AndroidSDK
cd AndroidSDK
wget https://dl.google.com/android/repository/tools_r25.2.3-linux.zip
unzip tools_r25.2.3-linux.zip
./tools/bin/sdkmanager "build-tools;22.0.1" "platforms;android-22" "extras;google;m2repository" "extras;android;m2repository" "add-ons;addon-google_apis-google-22" "extras;google;google_play_services"
```

Accept license agreements, and run

```
export ANDROID_HOME=`pwd`
cd ..
```

You are now set for the next step.

### Build and install

Invoke following command in the root folder of the project. `BACKEND_URI` should point to the
IP address or the domain name of the backend, and `BACKEND_PORT` to the port of the backend.

```
$ (cd ./frontend/android; ./gradlew installDebug -PBACKEND_URI="<host>" -PBACKEND_PORT=<port> )
```

Example

```
$ (cd ./frontend/android; ./gradlew installDebug -PBACKEND_URI="10.1.2.3" -PBACKEND_PORT=443 )
```

The command will install the app to the attached device. The installed app
is a DEBUG version for convenience reasons.

### Features

* All project requirements are satisfied
* Challenge: OAuth login has been implemented
* Challenge: Context-awareness offline mode. REMOTE and BENCHMARK modes are
  disabled when offline. Note that due to OAuth login, login still requires
  an internet connection.

### How to recompile precompiled libraries?

The android application uses native libraries and a custom JNI wrapper for them.
For convenience reasons, these are provided in a precompiled form. Should you
want to recompile the libraries, you need to compile and configure dependencies
defined in `frontend/external/libtessbinding/jni/`. Then you may invoke
`ndk-build .` in `frontend/external/libtessbinding`

## Frontend App Directory Structure
----

/frontend/android/app/src/main/assets/tessdata:
	* Tesseract training data

/frontend/android/app/src/main/jniLibs:
	* Precompiled native JNI glue libraries

/frontend/android/app/src/main/java/mcc_2016_g05_p2/niksula/hut/fi/android:
	* UI screens (Activities) used by the app

/frontend/android/app/src/main/java/mcc_2016_g05_p2/niksula/hut/fi/ocrdriver:
	* Glue code linking UI logic and OCR operations

/frontend/android/app/src/main/java/mcc_2016_g05_p2/niksula/hut/fi/ocrengine:
	* Java side of JNI wrapper for tesseract

/frontend/android/app/src/main/java/mcc_2016_g05_p2/niksula/hut/fi/asynctools:
	* Utils for async results.

/frontend/android/app/src/main/java/mcc_2016_g05_p2/niksula/hut/fi/rpc:
	* Encapsulation of server connection state, its details and some utilities.
	* RemoteRPC implements the remote API over SSL to a real backend.
	* FakeRemoteRPC implements the remote API by faking results. This allows UI testing without actual server connection.
	* MinimalHTTPSClient reimplements a limited subset of *standard* library `HttpsURLConnection` to work around `HttpsURLConnection` bugs.
=======
