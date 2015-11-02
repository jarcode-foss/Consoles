#include <jni.h>
#include <jni_md.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <setjmp.h>
#include <pthread.h>

#include "engine.h"

void classreg(JNIEnv* env, const char* name, jclass* mem, jmp_buf err_buf) {
	jobject local = (*env)->FindClass(env, name);
	CHECKEX(env, err_buf);
	if (local == 0) {
		fprintf(stderr, "\ncould not init class (%s)\n", name);
		exit(-1);
	}
	*mem = (*env)->NewGlobalRef(env, local);
	CHECKEX(env, err_buf);
	(*env)->DeleteLocalRef(env, local);
	CHECKEX(env, err_buf);
	if (!(*mem)) longjmp(err_buf, 1);
}
inline jint throw(JNIEnv* env, const char* message) {
	if (exclass)
		return (*env)->ThrowNew(env, exclass, message);
	else return 0;
}
inline void engine_swap(lua_State* state, int a, int b) {
	// make a copy of both elements
	lua_pushvalue(state, a);
	lua_pushvalue(state, b > 0 ? b : b - 1);
	// replace top value (b) at position a, and pop
	lua_replace(state, a > 0 ? a : a - 2);
	// replace new top value (a) at position b, and pop
	lua_replace(state, b > 0 ? b : b - 1);
}

JNIEXPORT void JNICALL Java_jni_LuaEngine_pthread_1name(JNIEnv* env, jobject this, jstring jname) {
	const char* characters = (*env)->GetStringUTFChars(env, jname, 0);
	pthread_t current = pthread_self();
	if (!current) {
		throw(env, "unable to get current POSIX thread");
		return;
	}
	char buf[16] = {0};
	size_t len = strlen(characters);
	memmove(buf, characters, len > 15 ? 15 : len);
	pthread_setname_np(current, buf);
	(*env)->ReleaseStringUTFChars(env, jname, characters);
}
