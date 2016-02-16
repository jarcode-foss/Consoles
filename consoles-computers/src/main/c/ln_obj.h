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
        class_##C = classreg(env, P, &class_##C, handle);               \
        id_##C##_constructor = method_resolve(env, class_##C, "<init>", "JJ" V, handle); \
    }                                                                   \
    jobject ln_new_##C(JNIEnv* env, ...) {                              \
        va_list argptr;                                                 \
        va_start(argptr, env);                                          \
        jobject ret = (*env)->NewObjectV(env, class_##C, id_##C##_constructor, \
                                        sizeof(C), (jlong) N, argptr);  \
        va_end(argptr);                                                 \
        return ret;                                                     \
    }

#define ln_setup(E, C, H)                       \
    do {                                        \
        ln_init(E, H);                          \
        ln_setup_##C(E, H);                     \
    } while (0)

#define ln_new(E, C, ...) ln_new_##C(E, ##__VA_ARGS__)
#define ln_struct(E, C, O) ((C*) ln_getdata(E, O))

#define ln_newid(I, N) ((ln_id) (N << 8) | I)

typedef ln_id uint16_t;

static inline jobject ln_resolve(JNIENv* env, jobject ln_obj, ln_id ref) {
    if (ref & 0xFF00) { /* upper 8 bits are not set */
        return ln_getref(env, ln_obj, (size_t) ref);
    }
    else { /* if they are set, use them to re-index again */
        return ln_getrefn(env, ln_obj, (size_t) (ref & 0xFF), (size_t) (ref & 0xFF00));
    }
}

/* data operations */

// returns __data as void*
extern void* ln_getdata(JNIEnv* env, jobject ln_obj);
// releases __data that was returned as void*
extern ln_releasedata(JNIEnv* env, jobject ln_obj, void* data);

/* general reference operations */

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
extern ln_releasedata(JNIEnv* env, jobject ln_obj, void* data);

/* buffer operations */

// performs __refs[idx] = new byte[size]
void ln_newrefbuf(JNIEnv* env, jobject ln_obj, size_t idx, size_t size);
// performs __refs[idx] = new byte[size] { ['init' data] }
void ln_newrefbufi(JNIEnv* env, jobject ln_obj, size_t idx, size_t size, void* init);
// resolves 'id' as byte[], returns the result as char*
void* ln_getrefbuf(JNIEnv* env, jobject ln_obj, ln_id id);
// resolves 'id' as byte[], releases elements pointed to by 'str'
void ln_releasebuf(JNIENv* env, jobject ln_obj, ln_id id, void* buf);

// basic init
extern void ln_init(JNIEnv* env, jmp_buf handle);
