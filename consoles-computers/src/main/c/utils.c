#define _GNU_SOURCE

#include <jni.h>
#include <jni_md.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <setjmp.h>
#include <pthread.h>
#include <unistd.h>

#include <stdarg.h>

#include "engine.h"

static int ABORT_SLEEP_TIME = 1000;

void classreg(JNIEnv* env, const char* name, jclass* mem, jmp_buf err_buf) {
    jobject local = (*env)->FindClass(env, name);
    CHECKEX(env, err_buf);
    if (local == 0) {
        fprintf(stderr, "\ncould not init class (%s)\n", name);
        engine_abort();
    }
    *mem = (*env)->NewGlobalRef(env, local);
    CHECKEX(env, err_buf);
    (*env)->DeleteLocalRef(env, local);
    CHECKEX(env, err_buf);
    if (!(*mem)) longjmp(err_buf, 1);
}
inline jint throwf(JNIEnv* env, const char* format, ...) {
    va_list argptr;
    jint ret;
    va_start(argptr, format);
    char buf[128];
    vsnprintf(buf, 128, format, argptr);
    ret = exclass ? (*env)->ThrowNew(env, exclass, buf) : 0;
    va_end(argptr);
    return ret;
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

inline void engine_abort() {
    fprintf(stdout, "LuaN: a fatal error occurred, aborting...\n");
    usleep(1000 * ABORT_SLEEP_TIME);
    exit(-1);
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

/* Only called if debugging is enabled and a JNI exception is raised where it is not expected */
void engine_fatal_exception(JNIEnv* env) {
    
    jthrowable ex = (*env)->ExceptionOccurred(env); // get exception
    (*env)->ExceptionClear(env); // clear for continued use of JNI

    // If the exception handler for joint has been setup, use it.
    if (id_exhandle && class_lua) {
        (*env)->CallStaticVoidMethod(env, class_lua, id_exhandle, ex);
    }
    else {
        printf("C: SEVERE: encountered an unexpected java exception, not able call handler\n");
    }
    printf("C: SEVERE: unexpected java exception, exiting...\n");
    
    abort(); // exit immidiately, preventing anything else from happening so we can debug.
}
