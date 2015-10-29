#include <jni.h>
#include <jni_md.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "engine.h"

void classreg(JNIEnv* env, const char* name, jclass* mem) {
	*mem = (*env)->FindClass(env, name);
	if (*mem == 0) {
		fprintf(stderr, "\ncould not init class (%s)\n", name);
		exit(-1);
	}
	*mem = (*env)->NewGlobalRef(env, *mem);
}
inline jint throw(JNIEnv* env, const char* message) {
	return (*env)->ThrowNew(env, exclass, message);
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
