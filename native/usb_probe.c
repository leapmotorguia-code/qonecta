#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <libusb.h>

/* Read descriptors only, on an Android-authorized open file descriptor.
 * No reset, configuration, claims, or transfers. Android owns the FD. */
JNIEXPORT jstring JNICALL Java_es_drivecore_cockpit_NativeUsb_descriptor(JNIEnv *env,jobject self,jint fd) {
  (void)self;
  char message[256];
  libusb_context *ctx=NULL;
  libusb_device_handle *handle=NULL;
  const struct libusb_version *v=libusb_get_version();
  struct libusb_init_option options[1] = {{.option=LIBUSB_OPTION_NO_DEVICE_DISCOVERY,.value.ival=1}};
  int rc=libusb_init_context(&ctx,options,1);
  if(rc==0)rc=libusb_wrap_sys_device(ctx,(intptr_t)fd,&handle);
  if(rc==0 && handle){
    struct libusb_device_descriptor desc;
    rc=libusb_get_device_descriptor(libusb_get_device(handle),&desc);
    if(rc==0)snprintf(message,sizeof(message),"LibUSB %u.%u.%u | VID:PID %04X:%04X | class %u | configurations %u | speed enum %d",v->major,v->minor,v->micro,desc.idVendor,desc.idProduct,desc.bDeviceClass,desc.bNumConfigurations,libusb_get_device_speed(libusb_get_device(handle)));
  }
  if(rc!=0||!handle)snprintf(message,sizeof(message),"LibUSB %u.%u.%u | error %s (%d)",v->major,v->minor,v->micro,libusb_error_name(rc),rc);
  if(handle)libusb_close(handle);
  if(ctx)libusb_exit(ctx);
  return (*env)->NewStringUTF(env,message);
}
