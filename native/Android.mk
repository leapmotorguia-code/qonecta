LOCAL_PATH := $(call my-dir)
DRIVECORE_JNI_PATH := $(LOCAL_PATH)
include $(DRIVECORE_JNI_PATH)/libusb/android/jni/libusb.mk
LOCAL_PATH := $(DRIVECORE_JNI_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := drivecore_usb
LOCAL_SRC_FILES := usb_probe.c
LOCAL_SHARED_LIBRARIES := libusb1.0
LOCAL_CFLAGS := -std=gnu11
include $(BUILD_SHARED_LIBRARY)
