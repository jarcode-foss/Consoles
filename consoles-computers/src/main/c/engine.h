
#include <jni.h>

#include <lua.h>

#include <stdlib.h>
#include <stdint.h>

#include <ffi.h>

#include <setjmp.h>

#include "ln_obj.h"

/*
 * Engine header file - this header (and the implementation) interfaces with the lua API and JNI for
 * type mapping and function wrapper magic.
 * 
 * The comments are mostly for myself, sorry if they don't make any sense. For the record, I use the
 * term 'lambda' strangely here, I'm _actually_ talking about an instance of on of the ***Func classes,
 * which is usually in the form of a lambda.
 * 
 * Two kinds of functions from Java can be wrapped:
 * 
 * - a reflected function, one that is defined by the macro METHOD_CLASS. It is passed straight into
 *   C as that instance, plus the instance of the object it is related to (if it is non-static).
 * 
 * - a lambda function, one that is one of the function classes (explained above). It is passed with
 *   list of argument types (java classes) and the instance of itself.
 * 
 * Functions are wrapped using libffi into their own functions at runtime.
 * 
 * Types are handled by having a C 'middleground' type, defined by the struct engine_value. Java
 * values are mapped to this type, as well as lua values.
 * 
 * Functions generated using libffi and engine values are not cleaned up until the instance of the
 * lua interpreter is closed (with the current implementation).
 * 
 */

#ifndef ENGINE_H_
#define ENGINE_H_

#if LUA_VERSION_NUM < 500
#error "Only Lua 5.x and LuaJIT 2.x versions are supported"
#endif

#define ENGINE_CDEBUG 0

// debugging macros
#if ENGINE_CDEBUG >= 1
// used to test the alignment of structures in maps/buffers
#define ENGINE_DEBUG_SIGNATURE 800328094372526
#define ENGINE_ASSERT_VALUE(v) ((engine_value*) v)->DEBUG_SIGNATURE == ENGINE_DEBUG_SIGNATURE
// will abort if an exception is unhandled
#define ASSERTEX(e) do { if ((*e)->ExceptionCheck(e) == JNI_TRUE) { engine_fatal_exception(e); } } while (0)
#else
#define ENGINE_ASSERT_VALUE(v) 1;
#define ASSERTEX(e) do {} while (0)
#endif // ENGINE_CDEBUG

// primitives
#define ENGINE_NULL 0
#define ENGINE_INTEGRAL 1
#define ENGINE_FLOATING 2
#define ENGINE_BOOLEAN 3

// strings are just converted from null-terminated to lua or through the JNI
#define ENGINE_STRING 4
// a java object, either passed directly back to Java or wrapped in lua as userdata
#define ENGINE_JAVA_OBJECT 5
// a java _lambda_ function, which means it is defined by one of the OneArgFunc, NoArgFunc, etc. types
#define ENGINE_JAVA_LAMBDA_FUNCTION 6
// a _reflected_ java function, meaning it is defined by a list of argument classes, the method id, and
// a global reference to the instance
#define ENGINE_JAVA_REFLECT_FUNCTION 7
// a lua function
#define ENGINE_LUA_FUNCTION 8
// an array of values (simplified table from lua)
#define ENGINE_ARRAY 9
// this is a special type, unlike every other type that is translated into an
// engine_value, the globals type just wraps access to lua_getglobal and
// lua_setglobal functions
#define ENGINE_LUA_GLOBALS 10

// this is OR'd on the value's type before free()'ing the value
#define ENGINE_MARKED 0x10

// this macro does not replace _all_ instances of the class string.
#define ENGINE_CLASS "java/lang/Class"

#define METHOD_CLASS "java/lang/reflect/Method"

// project-specific type classpaths. Any type that is part of the Java SE library is allowed to be used
// _without_ a macro, because it is assumed they will not change.

// if you are refactoring the code and these paths change, please update them and recompile the JNI library.
#define ENGINE_LUA_CLASS "ca/jarcode/ascript/Script"
#define ENGINE_ERR_CLASS "ca/jarcode/ascript/luanative/LuaNError"
#define ENGINE_VALUE_CLASS "ca/jarcode/ascript/luanative/LuaNScriptValue"
#define ENGINE_VALUE_INTERFACE "ca/jarcode/ascript/interfaces/ScriptValue"
#define ENGINE_OBJECT "ca/jarcode/ascript/luanative/LuaNObject"
#define ENGINE_THREAD_DATUM_CLASS "ca/jarcode/ascript/luanative/LuaNThreadDatum"

// we call all of our userdata objects an 'interface', since they work as a way to lookup methods
// from a java object.
#define ENGINE_USERDATA_TYPE "interface"

#define IS_ENGINE_FUNCTION(t) t == ENGINE_JAVA_LAMBDA_FUNCTION || t == ENGINE_JAVA_REFLECT_FUNCTION || t == ENGINE_LUA_FUNCTION
#define IS_NUMBER(t) t == ENGINE_BOOLEAN || t == ENGINE_FLOATING || t == ENGINE_INTEGRAL

// special lua table to store references to all passed functions
// lua programs can interact with this, but it's harmless. In fact,
// it could be used to speed up programs.
#define FUNCTION_REGISTRY "__functions"

// key and value for lua programs to find out if they are using a native or java implementation
#define ENGINE_TYPE_KEY "__impl"
#define ENGINE_TYPE "LuaN"

// check for java exception and jump to buffer if one has been thrown
#define CHECKEX(e, b) do { if ((*e)->ExceptionCheck(e) == JNI_TRUE) { longjmp(b, 1); } } while (0)

// translate engine value to a java object (unwrap and call Lua.translate(...))
// this is done on an array of engine values (v) and a java Class[] array (a)
// used for translating arguments in function calls
static jobject translate_java(JNIEnv* env, jobject type, jobject script_value, jmp_buf buf);
#define TOJAVA(e, v, a, i, b) translate_java(e, (*e)->GetObjectArrayElement(e, a, i), engine_wrap(env, v[i]), b)

// class 'Class'
extern jclass class_type;
extern jmethodID id_comptype;
extern jmethodID id_classname;

// class 'Lua'
extern jclass class_lua;
extern jmethodID id_translatevalue;
extern jmethodID id_translate;
extern jmethodID id_methodresolve;
extern jmethodID id_methodid;
extern jmethodID id_exhandle;

// class 'Method'
extern jclass class_method;
extern jmethodID id_methodcall;
extern jmethodID id_methodcount;
extern jmethodID id_methodtypes;

// class 'Object'
extern jclass class_object;
extern jmethodID id_hashcode;

// class 'LuaNError'
extern jclass exclass;

// class 'Throwable'
extern jclass class_ex;
extern jmethodID id_getmessage;

extern uint32_t function_index;

extern int engine_debug;

// A lua function is an index in a lua table of functions that have been (or need to be)
// exposed to a engine value. Functions are cached in this table when needed in C.
typedef int engine_luafunc;

static inline jobject translate_java
(JNIEnv* env, jobject type, jobject script_value, jmp_buf buf) {
    jobject obj = (*env)->CallStaticObjectMethod(env, class_lua, id_translate, type, script_value);
    CHECKEX(env, buf);
    return obj;
}

static inline jmethodID method_resolve
(JNIEnv* env, jclass type, const char* method, const char* signature, jmp_buf buf) {
    jmethodID ret = (*env)->GetMethodID(env, type, method, signature);
    CHECKEX(env, buf);
    return ret;
}

static inline jfieldID field_resolve
(JNIEnv* env, jclass type, const char* field, const char* signature, jmp_buf buf) {
    jfieldID ret = (*env)->GetFieldID(env, type, field, signature);
    CHECKEX(env, buf);
    return ret;
}

static inline jmethodID static_method_resolve
(JNIEnv* env, jclass type, const char* method, const char* signature, jmp_buf buf) {
    jmethodID ret = (*env)->GetStaticMethodID(env, type, method, signature);
    CHECKEX(env, buf);
    return ret;
}

/*
 * struct for information about a lambda function (usally passed by value)
 */
typedef struct {
    uint8_t args;
    uint8_t ret;
} engine_lambda_info;

// forward declaration
typedef struct engine_inst_ engine_inst;

/*
 * This is a function wrapper, a type passed to a special function in engine.c that handles all function calls.
 */
typedef struct {
    jobject obj_inst;            // lambda, or instance of object that a reflected method is accessing
    ffi_closure* closure;        // closure
    lua_CFunction func;          // function
    union {                      // type-unqiue data
        struct {
            jmethodID id;        // id of lambda method
            uint8_t ret;         // >0 if there is a return value
            jobject class_array; // Class[] global ref
        } lambda;
        struct {
            jobject method;      // Method instance
            long reflect_id;     // unqiue wrapper id
        } reflect;
    } data;
    uint8_t type;                // lambda or reflected
    uint8_t skip_first;          // skip first argument
    engine_inst* engine;
} engine_jfuncwrapper;

/*
 * An instance of this engine, associated with a lua interpreter instance. A new engine instance should
 * be created for every separate lua chunk to be run.
 */
struct engine_inst_ {
    lua_State* state;
    uint8_t closed;
    uint8_t restricted;
    
    // all function wrappers for this instance (only cleaned up at the end of the VM's lifecycle)
    // there is only one wrapper per unique function passed to lua
    engine_jfuncwrapper** wrappers;
    size_t wrappers_amt;
    
    // assigned (never used) before a chunk is executed in Lua
    // this is used for function wrappers that need to find the environment they are running in
    JNIEnv* runtime_env;
    
    // this is the ffi closure used for the hook function
    ffi_closure* closure;
    
    // this is a flag used by hook function to determine if the VM has been killed
    volatile uint8_t killed;
    
    // hook function
    lua_Hook hook;

    // the last time an interrupt was fired (ms)
    volatile unsigned long last_interrupt;

    int interval; // interval in which a hook is executed
};

/*
 * reflected java function
 */
typedef struct {
    jobject obj_inst;           // NULL if static, global ref
    jobject reflect_method;     // Method instance, global ref
} engine_rfunc;

/*
 * lambda java function
 */
typedef struct {
    jobject lambda;       // lambda instance, global ref
    jobject class_array;  // java array of classes (Class[])
} engine_lfunc;

/*
 * tiny userdata type passed to lua, managed completely by lua
 */
typedef struct {
    jobject obj;          // jobject, global reference, cleaned up by lua
    engine_inst* engine;
    uint8_t released;
} engine_userdata;

// forward declaration
typedef struct engine_value_ engine_value;

/*
 * engine data, used for easy sizeof's and a member of the engine_value struct
 */

// reference indexes/slots for values
#define LN_VALUE_REF_PRIMARY 0
#define LN_VALUE_REF_SECONDARY 1

// macro for getting array element at index 'N', need to be cleaned up (delete local refs)
#define LN_VALUE_ELEM(E, V, N) (ln_getrefn(E, V->ref, LN_VALUE_REF_PRIMARY, N))

// primary and secondary reference slots, need to be cleaned up (delete local refs)
#define LV_VALUE_P(E, V) ln_getref(E, V->ref, LN_VALUE_REF_PRIMARY)
#define LV_VALUE_S(E, V) ln_getref(E, V->ref, LN_VALUE_REF_SECONDARY)

// designated initilizers for lfunc and rfunc, need to be cleaned up (delete local refs)
#define LN_VALUE_LFUNC(E, V) ((engine_lfunc) {.lambda = LV_VALUE_P(E, V), .class_array = LV_VALUE_S(E, V)})
#define LN_VALUE_RFUNC(E, V) ((engine_lfunc) {.obj_inst = LV_VALUE_P(E, V), .reflect_method = LV_VALUE_S(E, V)})

// helper for cleaning up values
#define LN_VALUE_CLEANUP(E, V) ln_releasedata(E, V->ref, V)

/* Returns the C structure for a value */
static inline engine_value* engine_unwrap(JNIEnv* env, jobject obj) {
    engine_value* ret = ln_struct(env, engine_value, obj);
    ret->ref = obj;
    return ret;
}

/* Returns the Java object for a value */
#define engine_wrap(E, V) (V->ref)

typedef union engine_data_ {
    long i;               // int, long, short, boolean, or byte
    double d;             // float or double
    
                          // objects, engine_lfunc, and engine_rfunc are resolved
                          // from LN_VALUE_REF_PRIMARY and LN_VALUE_REF_SECONDARY
    
                          // strings are the same, except they are stored as byte[]
                          // in the java reference stack (primary)

    engine_luafunc luafunc;
    
    size_t arraylen;      // if value is an array, the length is only stored
                          // values are stored in __refs[0][n]
    
    lua_State* state;     // pointer to pointer of lua_State at runtime
} engine_data;

struct engine_value_ {
#if ENGINE_CDEBUG > 0
    uint64_t DEBUG_SIGNATURE;
#endif // ENGINE_CDEBUG
    engine_data data;    // data
    uint8_t type;        // type of value
    jobject ref;         // reference to a local object for this value, set when the structure is in use
    
    // engine instance, we need this to parse and clean the value stack
    // if this value is null, we assume that this is a shared value (to be used across VMs)
    engine_inst* inst;
};

/* Set up method ids and class references */
extern void setup_value(JNIEnv* env, jmp_buf handle);
extern void thread_datum_init(JNIEnv* env, jmp_buf handle);

/* Create engine value */
extern engine_value* engine_newvalue(JNIEnv* env, engine_inst* inst);
extern engine_value* engine_newsharedvalue(JNIEnv* env);

/* Same as calling release() from Java */
extern void engine_releasevalue(JNIEnv* env, engine_value* value);

/* Get information about a Java lambda (ascript.func.* types) */
extern void engine_getlambdainfo(JNIEnv* env, engine_inst* inst, jclass jfunctype,
                                 jobject class_array, engine_lambda_info* buf);

/* Pop a value from the Lua C stack */
extern engine_value* engine_popvalue(JNIEnv* env, engine_inst* inst, lua_State* state);

/* Handles the registry for an engine value that points to a Lua function */
extern void engine_handleregistry(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* v);

/* Push values to the Lua C stack */
extern void engine_pushobject(JNIEnv* env, engine_inst* inst, lua_State* state, jobject obj);
extern void engine_pushlambda(JNIEnv* env, engine_inst* inst, jobject jfunc, jobject class_array);
extern void engine_pushreflect(JNIEnv* env, engine_inst* inst, jobject reflect_method, jobject obj_inst);
extern void engine_pushvalue(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* value);

/* Copy engine value, creating a new associated java object and copies all containing references */
/* Performs a deep copy for arrays */
extern engine_value* value_copy(JNIEnv* env, engine_value* value);

/* Call function on the Lua C stack */
extern engine_value* engine_call(JNIEnv* env,engine_inst* inst, lua_State* state, int nargs);

/* Throw LuaNError to Java, with the given message */
extern jint throw(JNIEnv* env, const char* message);
extern jint throwf(JNIEnv* env, const char* format, ...);

/* Abruptly abort */
extern void engine_abort(void);

/* Abruptly abort, used when Java throws an exception when not expected to (ASSERTEX macro). */
extern void engine_fatal_exception(JNIEnv* env);

/* Register global reference to Java class, if something fails, jmp_buf is jumped to. */
extern void classreg(JNIEnv* env, const char* name, jclass* mem, jmp_buf buf);

/* Swaps two values on the Lua C stack, at index 'a' and 'b'. */
extern void engine_swap(lua_State* state, int a, int b);

#endif // ENGINE_H_
