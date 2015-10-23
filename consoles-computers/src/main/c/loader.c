#include <jni.h>
#include <jni_md.h>
#include <NLoader.h>
#include <stdio.h>
#include <dlfcn.h>

#include "loader.h"

JNIEXPORT jlong JNICALL Java_jni_NLoader_dlopen(JNIEnv *env, jobject this, jstring str) {

	const char* libpath = (*env)->GetStringUTFChars(env, str, 0);
	(*env)->ReleaseStringUTFChars(env, str, libpath);

	printf("\nloading native: '%s'", libpath);

	void* handle = dlopen(libpath, RTLD_LAZY);
	return (jlong) handle;
}

JNIEXPORT jint JNICALL Java_jni_NLoader_dlclose(JNIEnv *env, jobject this, jlong address) {
	void* handle = (void*) address;
	return dlclose(handle);
}