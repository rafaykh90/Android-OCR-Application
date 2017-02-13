LOCAL_PATH := $(call my-dir)/..
include $(CLEAR_VARS)

LOCAL_MODULE := libtessbinding-native
LOCAL_C_INCLUDES := \
	/home/ubuntu/Documents/MCC/nova/tesseract-3.05/ \

LOCAL_C_INCLUDES += \
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/api \
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/ccmain\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/ccstruct\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/ccutil\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/classify\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/cutil\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/dict\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/image\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/textord\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/third_party\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/wordrec\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/opencl\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/viewer\
  /home/ubuntu/Documents/MCC/nova/tesseract-3.05/../leptonica/include

LOCAL_SRC_FILES := \
	libtessbinding.cpp \

LOCAL_CFLAGS := -O2 -W -Wall -Wno-unused-parameter -Wno-sign-compare -Wno-pointer-sign
LOCAL_LDLIBS := -lz -llog
LOCAL_STATIC_LIBRARIES = tesseract-native leptonica-native

TARGET_PLATFORM := android-22
include $(BUILD_SHARED_LIBRARY)

# -------------------------
include $(CLEAR_VARS)
LOCAL_MODULE := tesseract-native
LOCAL_SRC_FILES := /path/to/tesseract-3.05/libs/$(TARGET_ARCH_ABI)/libtesseract-all.a
include $(PREBUILT_STATIC_LIBRARY)

# --------------
include $(CLEAR_VARS)
LOCAL_MODULE := leptonica-native
LOCAL_SRC_FILES := /path/to/leptonica-1.73/libs/$(TARGET_ARCH_ABI)/libleptonica-native.a
include $(PREBUILT_STATIC_LIBRARY)
