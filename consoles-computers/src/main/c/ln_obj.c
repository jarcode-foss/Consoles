
#include <jni.h>

#include <stdlib.h>
#include <stdio.h>

#include "engine.h"

static jclass class_lobj;
static jclass class_object;
static jfieldID id_odata, id_orefs;

static uint8_t setup = 0;

static jboolean make_jni_copies = JNI_FALSE;

void ln_init(JNIEnv* env, jmp_buf handle) {
    if (!setup) {
        classreg(env, ENGINE_OBJECT, &class_lobj, handle);
        classreg(env, "java/lang/Object", &class_object, handle);
        id_odata = field_resolve(env, class_lobj, "__data", "[B", handle);
        id_orefs = field_resolve(env, class_lobj, "__refs", "[Ljava/lang/Object;", handle);
        setup = 1;
    }
}

// performs __refs[idx]
jobject ln_getref(JNIEnv* env, jobject ln_obj, size_t idx) {
    jobject arr_obj = (*env)->GetObjectField(env, ln_obj, id_orefs);
    jobject result = (*env)->GetObjectArrayElement(env, arr_obj, (jsize) idx);
    (*env)->DeleteLocalRef(arr_obj);
    return result;
}

// performs __refs[idx] = ref
void ln_setref(JNIEnv* env, jobject ln_obj, size_t idx, jobject ref) {
    jobject arr_obj = (*env)->GetObjectField(env, ln_obj, id_orefs);
    (*env)->SetObjectArrayElement(env, arr_obj, (jsize) idx, ref);
    (*env)->DeleteLocalRef(env, arr_obj);
}

// performs __refs[idx] = new Object[size]
void ln_newrefstack(JNIEnv* env, jobject ln_obj, size_t idx, size_t size) {
    jobject arr_obj = (*env)->GetObjectField(env, ln_obj, id_orefs);
    jobject new_arr = (*env)->NewObjectArray(env, size, class_object, NULL);
    (*env)->SetObjectArrayElement(env, arr_obj, (jsize) idx, new_arr);
    (*env)->DeleteLocalRef(env, arr_obj);
    (*env)->DeleteLocalRef(env, new_arr);
}

// performs __refs[idx][nidx], assuming __refs[idx]'s type == Object[]
jobject ln_getrefn(JNIEnv* env, jobject ln_obj, size_t idx, size_t nidx) {
    jobject arr_obj = (*env)->GetObjectField(env, ln_obj, id_orefs);
    jobject element = (*env)->GetObjectArrayElement(env, arr_obj, (jsize) idx);
    jobject n_element = (*env)->GetObjectArrayElement(env, element, (jsize) nidx);
    (*env)->DeleteLocalRef(env, element);
    (*env)->DeleteLocalRef(env, arr_obj);
    return n_element;
}

// returns __data as void*
void* ln_getdata(JNIEnv* env, jobject ln_obj) {
    jobject arr_obj = (*env)->GetObjectField(env, ln_obj, id_odata);
    jobject result = (void*) (*env)->GetByteArrayElements(env, arr_obj, &make_jni_copies);
    (*env)->DeleteLocalRef(env, arr_obj);
    return result;
}

// releases structure data
void ln_releasedata(JNIEnv* env, jobject ln_obj, void* data) {
    jobject arr_obj = (*env)->GetObjectField(env, ln_obj, id_odata);
    (*env)->ReleaseByteArrayElements(env, arr_obj, (jbyte*) data, 0);
    (*env)->DeleteLocalRef(env, arr_obj);
}
