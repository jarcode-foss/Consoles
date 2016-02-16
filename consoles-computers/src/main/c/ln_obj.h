#include <jni.h>

#include "engine.h"

// same as below, except with no extra signature
#define LN_DECLARE(C, P, N) LN_DECLARE_C(C, P, "", N);
/*
  'C' is the typedef'd name of a structure, 'P' is the Java classpath,
  , 'V' is the signature (after "JJ"), and 'N' is the reference stack size.
*/
#define LN_DECLARE_C(C, P, V, N)                                        \
    static jmethodID id_##C##_constructor;                              \
    static jclass class_##C;                                            \
    void ln_setup_##C(JNIEnv* env, jmp_buf handle) {                    \
        class##C = classreg(env, P, &class_##C, handle);                \
        id_##C##_constructor = method_resolve(env, class_##C, "<init>", "JJ" V, handle); \
    }                                                                   \
    jobject ln_new_##C(JNIEnv* env) {                                   \
        (*env)->NewObject(env, class_##C, id_##C##_constructor, sizeof(C), (jlong) N); \
    }

#define ln_setup(E, C, H)                       \
    do {                                        \
        ln_init(E, H);                          \
        ln_setup_##C(E, H);                     \
    } while (0)

#define ln_new(E, C) ln_new_##C(E)
#define ln_struct(E, C, O) ((C) ln_getdata(E, O))

// performs __refs[idx]
extern jobject ln_getref(JNIEnv* env, jobject ln_obj, size_t idx);
// performs __refs[idx] = ref
extern void ln_setref(JNIEnv* env, jobject ln_obj, size_t idx, jobject ref);
// performs __refs[idx][nidx], assuming __refs[idx]'s type == Object[]
extern jobject ln_getrefn(JNIEnv* env, jobject ln_obj, size_t idx, size_t nidx);
// performs __refs[idx] = new Object[size]
extern void ln_newrefstack(JNIEnv* env, jobject ln_obj, size_t idx, size_t size);
// returns __data as void*
extern void* ln_getdata(JNIEnv* env, jobject ln_obj);
// releases __data that was returned as void*
extern ln_releasedata(JNIEnv* env, jobject ln_obj, void* data)
// basic init
extern void ln_init(JNIEnv* env, jmp_buf handle);
