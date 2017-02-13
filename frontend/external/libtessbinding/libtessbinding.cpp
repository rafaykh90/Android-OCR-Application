/*
 * LibTessBinding - JNI binding for tesseract.
 */

#include <jni.h>
#include <errno.h>
#include <stdint.h>
#include <stdarg.h>
#include <android/log.h>

#define NO_CUBE_BUILD
#include <baseapi.h>

#define JNI_EXPORT extern "C"
#define nullptr NULL
#define LOG_TAG "libtessbinding"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "[libtessbinding] " __VA_ARGS__)

/* Utils */

static char* copyJavaStringToCString (JNIEnv* env, jstring string)
{
	if (string == nullptr)
		return nullptr;

	const char* jniStringPtr = env->GetStringUTFChars(string, JNI_FALSE);
	char* cString = strdup(jniStringPtr);
	env->ReleaseStringUTFChars(string, jniStringPtr);

	return cString;
}

/* API */

/* LibTessBinding::open */
JNI_EXPORT jlong Java_mcc_12016_1g05_1p2_niksula_hut_fi_ocrengine_LibTessBinding_open (JNIEnv* env, jclass klass, jstring dataPathJString)
{
	const char* dataPath = copyJavaStringToCString(env, dataPathJString);
	if (!dataPath)
	{
		LOG("open failed, invalid data path");
		return 0;
	}

	tesseract::TessBaseAPI* api = new tesseract::TessBaseAPI();
	if (api->Init(dataPath, "eng"))
	{
		LOG("open failed, init failed");
		delete api;
		api = nullptr;
	}

	LOG("open success");

	free((void*)dataPath);
	return reinterpret_cast<jlong>(api);
}

/* LibTessBinding::close */
JNI_EXPORT void Java_mcc_12016_1g05_1p2_niksula_hut_fi_ocrengine_LibTessBinding_close (JNIEnv* env, jclass klass, jlong handle)
{
	tesseract::TessBaseAPI* api = reinterpret_cast<tesseract::TessBaseAPI*>(handle);
	if (!api)
	{
		LOG("close failed, no api");
		return;
	}

	LOG("closing api");

	api->End();
	delete api;
}

/* LibTessBinding::process */
JNI_EXPORT jstring Java_mcc_12016_1g05_1p2_niksula_hut_fi_ocrengine_LibTessBinding_process (JNIEnv* env, jclass klass, jlong handle, jintArray pixelData, jint width, jint height)
{
	tesseract::TessBaseAPI* api = reinterpret_cast<tesseract::TessBaseAPI*>(handle);
	if (!api)
	{
		LOG("process failed, no api");
		return nullptr;
	}

	int		bytesPerPixel	= 4;
	int		rowStride		= width * bytesPerPixel;
	jint*	imageData		= env->GetIntArrayElements(pixelData, nullptr);

	if (!imageData)
	{
		LOG("process failed, no data");
		return nullptr;
	}

	api->SetImage((unsigned char*)imageData, width, height, bytesPerPixel, rowStride);
	const char* ocrdTextUtf8 = api->GetUTF8Text();

	env->ReleaseIntArrayElements(pixelData, imageData, JNI_ABORT);

	if (!ocrdTextUtf8)
	{
		LOG("process failed, did not recognize anything");
		return nullptr;
	}

	LOG("process success");

	// UTF8 is subset of Modified UTF8, all is ok
	jstring obrdString = env->NewStringUTF(ocrdTextUtf8);
	delete [] ocrdTextUtf8;
	return obrdString;
}
