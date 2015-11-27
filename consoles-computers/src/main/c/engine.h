
#include <jni.h>

#include <lua.h>

#include <stdint.h>

#include <ffi.h>

#include <setjmp.h>

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

// this macro does not replace _all_ instances of the class string.
#define ENGINE_CLASS "java/lang/Class"

#define METHOD_CLASS "java/lang/reflect/Method"

// project-specific type classpaths. Any type that is part of the Java SE library is allowed to be used
// _without_ a macro, because it is assumed they will not change.

// if you are refactoring the code and these paths change, please update them and recompile the JNI library.
#define ENGINE_LUA_CLASS "ca/jarcode/consoles/computer/interpreter/Lua"
#define ENGINE_ERR_CLASS "ca/jarcode/consoles/computer/interpreter/luanative/LuaNError"
#define ENGINE_VALUE_CLASS "ca/jarcode/consoles/computer/interpreter/luanative/LuaNScriptValue"
#define ENGINE_VALUE_INTERFACE "ca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue"

#define IS_ENGINE_FUNCTION(t) t == ENGINE_JAVA_LAMBDA_FUNCTION || t == ENGINE_JAVA_REFLECT_FUNCTION || t == ENGINE_LUA_FUNCTION

// special lua table to store references to all passed functions
// lua programs can interact with this, but it's harmless. In fact,
// it could be used to speed up programs.
#define FUNCTION_REGISTRY "__functions"

// key and value for lua programs to find out if they are using a native or java implementation
#define ENGINE_TYPE_KEY "__impl"
#define ENGINE_TYPE "native"

// check for java exception and jump to buffer if one has been thrown
#define CHECKEX(e, b) do { if ((*e)->ExceptionCheck(e) == JNI_TRUE) { longjmp(b, 1); } } while (0)

// translate engine value to a java object (unwrap and call Lua.translate(...))
// this is done on an array of engine values (v) and a java Class[] array (a)
// used for translating arguments in function calls
#define TOJAVA(e, v, a, i) (*e)->CallStaticObjectMethod(e, class_lua, id_translate, \
                                                        (*e)->GetObjectArrayElement(e, a, i), \
                                                        engine_wrap(env, v[i]))

static inline jmethodID method_resolve
(JNIEnv* env, jclass type, const char* method, const char* signature, jmp_buf buf) {
	jmethodID ret = (*env)->GetMethodID(env, type, method, signature);
	CHECKEX(env, buf);
	return ret;
}

static inline jmethodID static_method_resolve
(JNIEnv* env, jclass type, const char* method, const char* signature, jmp_buf buf) {
	jmethodID ret = (*env)->GetStaticMethodID(env, type, method, signature);
	CHECKEX(env, buf);
	return ret;
}
		

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
	jobject obj_inst; // lambda, or instance of object that a reflected method is accessing (can be null)
	ffi_closure* closure; // closure
	lua_CFunction func; // function
	union { // type-unqiue data
		struct {
			jmethodID id; // id of lambda method
			uint8_t ret; // >0 if there is a return value
			jobject class_array; // Class[] global ref
		} lambda;
		struct {
			jobject method; // Method instance
			long reflect_id; // unqiue wrapper id
		} reflect;
	} data;
	uint8_t type; // lambda or reflected
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
	
	// this is used, mainly by userdatums passed to lua, to keep track of objects
	// that still need their global references deleted (these are copies of those references)
	jobject* floating_objects;
	size_t floating_objects_amt;
	
	// this is the ffi closure used for the hook function
	ffi_closure* closure;
	
	// this is a flag used by hook function to determine if the VM has been killed
	volatile uint8_t killed;
	
	// hook function
	lua_Hook hook;
};


// IF COPIED MAKE SURE TO CREATE NEW GLOBAL REFS

/*
 * reflected java function
 */
typedef struct {
	jobject obj_inst; // NULL if static, global ref
	jobject reflect_method; // Method instance, global ref
} engine_rfunc;

/*
 * lambda java function
 */
typedef struct {
	jobject lambda; // lambda instance, global ref
	jobject class_array; // java array of classes (Class[])
} engine_lfunc;

/*
 * tiny userdata type passed to lua, managed completely by lua
 */
typedef struct {
	jobject obj; // jobject, global _floating_ reference
	engine_inst* engine;
	uint8_t released;
} engine_userdata;

// forward declaration
typedef struct engine_value_ engine_value;

/*
 * engine data, used for easy sizeof's and a member of the engine_value struct
 */
typedef union engine_data_ {
	long i; // int, long, short, boolean, or byte
	double d; // float or double
	char* str; // null-terminated string
	jobject obj; // object (userdata)
	engine_lfunc lfunc; // java function (lambda)
	engine_luafunc func; // lua function
	engine_rfunc rfunc; // reflected java function
	struct { // array
		engine_value** values; // pointer to block of memory containing pointers to engine values
		uint32_t length; // length of memory block
	} array;
	lua_State* state; // pointer to pointer of lua_State at runtime
} engine_data;

/*
 * a 'middleground' engine value
 * 
 * LuaJIT has no copies of this value, this is _only_ associated with a java object in a pair map.
 */
struct engine_value_ {
	engine_data data; // data
	uint8_t type; // type of value
	
	// engine instance, we need this to parse and clean the value stack
	// if this value is null, we assume that this is a shared value (to be used across VMs)
	engine_inst* inst;
};

// sets up method ids and class references
extern void setup_value(JNIEnv* env, jmp_buf handle);

// create engine value
extern engine_value* engine_newvalue(JNIEnv* env, engine_inst* inst);
extern engine_value* engine_newsharedvalue(JNIEnv* env);

extern void engine_releasevalue(JNIEnv* env, engine_value* value);

extern void engine_clearvalues(JNIEnv* env, engine_inst* inst);
// unwraps the java script value to C engine_value
// NOTHING IS ALLOCATED, it just looks up a pair and finds an existing allocation to the value
extern engine_value* engine_unwrap(JNIEnv* env, jobject obj);
// same thing, just in reverse
extern jobject engine_wrap(JNIEnv* env, engine_value* value);
// get info from lambda function class
extern engine_lambda_info engine_getlambdainfo(JNIEnv* env, engine_inst* inst, jclass jfunctype, jobject class_array);

// wrap lua value to script value
// this operates on the value on the top of the lua stack, popping it after
extern engine_value* engine_popvalue(JNIEnv* env, engine_inst* inst, lua_State* state);
extern void engine_pushvalue(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* value);
// pushes a lambda onto the stack as a wrapped C function
extern void engine_pushlambda(JNIEnv* env, engine_inst* inst, jobject jfunc, jobject class_array);
extern void engine_pushreflect(JNIEnv* env, engine_inst* inst, jobject reflect_method, jobject obj_inst);

extern void engine_addfloating(engine_inst* inst, jobject reference);
extern void engine_removefloating(engine_inst* inst, jobject reference);

extern void engine_handleregistry(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* v);
extern void engine_pushobject(JNIEnv* env, engine_inst* inst, lua_State* state, jobject obj);

extern engine_value* engine_call(JNIEnv* env, engine_inst* inst, lua_State* state, int nargs);

extern jint throw(JNIEnv* env, const char* message);

extern void engine_abort(void);

// utils

// register global ref to class at memory
extern void classreg(JNIEnv* env, const char* name, jclass* mem, jmp_buf buf);

extern void engine_swap(lua_State* state, int a, int b);

#endif // ENGINE_H_
