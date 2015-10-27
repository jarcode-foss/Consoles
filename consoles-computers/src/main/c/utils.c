#include <jni.h>
#include <jni_md.h>
#include <stdio.h>
#include <stdlib.h>

#include "engine.h"

void classreg(JNIEnv* env, const char* name, jclass* mem) {
	*mem = (*env)->FindClass(env, name);
	if (*mem == 0) {
		fprintf(stderr, "\ncould not init class (%s)\n", name);
		exit(-1);
	}
	*mem = (*env)->NewGlobalRef(env, *mem);
}
