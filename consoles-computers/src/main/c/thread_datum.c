#include <jni.h>
#include <jni_md.h>

#include <ffi.h>

#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>

#include <string.h>

#include <pthread.h>

#include "engine.h"

static jclass thread_datum_type;
static jfieldID id_address;

static uint8_t setup = 0;

typedef struct {
    pthread_key_t key;
    pthread_once_t key_once;
    void (*setup) (void);
    ffi_closure* closure;
} thread_datum;

static ffi_cif bind_cif;

/*

static inline bool atomic_flagread(volatile uint8_t* b) {
    bool v = *b;
    __sync_synchronize();
    return v;
}

static inline void atomic_flagwrite(volatile uint8_t* b, uint8_t v) {
    __sync_synchronize();
   *b = v;
}

*/

static inline thread_datum* findnative(JNIEnv* env, jobject ref) {
    jlong value = (*env)->GetLongField(env, ref, id_address);
    if (value) {
        return (thread_datum*) (intptr_t) value;
    }
    else {
        throw(env, "C: could not find internal value");
        return 0;
    }
}

static inline void setup_closure(thread_datum* d) {
    int ret = pthread_key_create(&(d->key), 0);
    if (ret) {
        fprintf(stderr, "\nFailed pthread key creation for thread datum (%d)\n", ret);
        abort();
    }
}

static inline void abort_ffi() {
    fprintf(stderr, "\nFailed FFI call for thread datum\n");
    fflush(stderr);
    abort();
}

static void handle_bind(ffi_cif* cif, void* ret, void* args[], void* user_data) {
    setup_closure((thread_datum*) user_data);
}

void thread_datum_init(JNIEnv* env, jmp_buf handle) {
    if (!setup) {
        classreg(env, ENGINE_THREAD_DATUM_CLASS, &thread_datum_type, handle);
        id_address = field_resolve(env, thread_datum_type, "__address", "J", handle);
        
        ffi_status ret;
        switch (ret = ffi_prep_cif(&bind_cif, FFI_DEFAULT_ABI, 0, &ffi_type_void, NULL)) {
        case FFI_OK:
            break;
        case FFI_BAD_TYPEDEF:
            fprintf(stderr, "\nffi_prep_cif returned FFI_BAD_TYPEDEF\n");
            longjmp(handle, 1);
            break;
        case FFI_BAD_ABI:
            fprintf(stderr, "\nffi_prep_cif returned FFI_BAD_ABI\n");
            longjmp(handle, 1);
            break;
        default:
            fprintf(stderr, "\nffi_prep_cif returned %d\n", (int) ret);
            longjmp(handle, 1);
            break;
        }
        setup = 1;
    }
}

/*
 * Class:     ca_jarcode_ascript_luanative_LuaNThreadDatum
 * Method:    init
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_ascript_luanative_LuaNThreadDatum_init
(JNIEnv* env, jobject this) {
    if (!setup) {
        jmp_buf handle;
        if (setjmp(handle)) {
            throw(env, "C: failed to prepare FFI caller interface");
            return;
        }
        thread_datum_init(env, handle);
    }
    thread_datum* d = malloc(sizeof(thread_datum));
    d->key_once = PTHREAD_ONCE_INIT;
    void* func_address;
    d->closure = ffi_closure_alloc(sizeof(ffi_closure), &func_address);
    ffi_status ret;
    switch (ret = ffi_prep_closure_loc(d->closure, &bind_cif, &handle_bind, d, func_address)) {
        case FFI_OK:
            break;
        case FFI_BAD_TYPEDEF:
            fprintf(stderr, "\nffi_prep_closure_loc returned FFI_BAD_TYPEDEF\n");
            abort_ffi();
            break;
        case FFI_BAD_ABI:
            fprintf(stderr, "\nffi_prep_closure_loc returned FFI_BAD_ABI\n");
            abort_ffi();
            break;
        default:
            fprintf(stderr, "\nffi_prep_closure_loc returned %d\n", (int) ret);
            abort_ffi();
            break;
    }
    d->setup = func_address;
    (*env)->SetLongField(env, this, id_address, (jlong) (intptr_t) d);
}

/*
 * Class:     ca_jarcode_ascript_luanative_LuaNThreadDatum
 * Method:    destroyContext
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_ascript_luanative_LuaNThreadDatum_destroyContext
(JNIEnv* env, jobject this) {
    thread_datum* d = findnative(env, this);
    pthread_once(&(d->key_once), d->setup);
    jobject* ptr = pthread_getspecific(d->key);
    if (ptr) {
        (*env)->DeleteGlobalRef(env, *ptr);
        free(ptr);
    }
}

/*
 * Class:     ca_jarcode_ascript_luanative_LuaNThreadDatum
 * Method:    get
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_ascript_luanative_LuaNThreadDatum_get
(JNIEnv* env, jobject this) {
    thread_datum* d = findnative(env, this);
    pthread_once(&(d->key_once), d->setup);
    jobject* ptr = pthread_getspecific(d->key);
    if (!ptr) {
        ptr = malloc(sizeof(jobject));
        memset(ptr, 0, sizeof(jobject));
        pthread_setspecific(d->key, ptr);
    }
    return *ptr;
}

/*
 * Class:     ca_jarcode_ascript_luanative_LuaNThreadDatum
 * Method:    set
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_ascript_luanative_LuaNThreadDatum_set
(JNIEnv* env, jobject this, jobject value) {
    thread_datum* d = findnative(env, this);
    pthread_once(&(d->key_once), d->setup);
    jobject* ptr = pthread_getspecific(d->key);
    if (!ptr) {
        ptr = malloc(sizeof(jobject));
        memset(ptr, 0, sizeof(jobject));
        pthread_setspecific(d->key, ptr);
    }
    if (!*ptr) {
        (*env)->DeleteGlobalRef(env, *ptr);
    }
    value = (*env)->NewGlobalRef(env, value);
    *ptr = value;
}

/*
 * Class:     ca_jarcode_ascript_luanative_LuaNThreadDatum
 * Method:    release
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_ascript_luanative_LuaNThreadDatum_release
(JNIEnv* env, jobject this) {
    thread_datum* d = findnative(env, this);
    ffi_closure_free(d->closure);
    free(d);
}
