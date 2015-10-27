
#include <jni.h>

#include <lua.h>

#include <stdint.h>

#include <ffi.h>

#ifndef ENGINE_H_
#define ENGINE_H_

// primitives
#define ENGINE_INTEGRAL = 0;
#define ENGINE_FLOATING = 1;
#define ENGINE_BOOLEAN = 2;

// strings are just converted from null-terminated to lua or through the JNI
#define ENGINE_STRING = 3;
// a java object, either passed directly back to Java or wrapped in lua as userdata
#define ENGINE_JAVA_OBJECT = 4;
// a java _lambda_ function, which means it is defined by one of the OneArgFunc, NoArgFunc, etc. types
#define ENGINE_JAVA_LAMBDA_FUNCTION = 5;
// a _reflected_ java function, meaning it is defined by a list of argument classes, the method id, and
// a global reference to the instance
#define ENGINE_JAVA_REFLECT_FUNCTION = 6;
// a lua function
#define ENGINE_LUA_FUNCTION = 7;
// an array of values (simplified table from lua)
#define ENGINE_ARRAY = 8;

#define ENGINE_CLASS = "java/lang/Class"
#define ENGINE_LUA_CLASS "ca/jarcode/consoles/computer/interpreter/Lua"
#define ENGINE_ERR_CLASS = "ca/jarcode/consoles/computer/interpreter/luanative/LuaNError";
#define ENGINE_VALUE_CLASS = "ca/jarcode/consoles/computer/interpreter/luanative/LuaNScriptValue"
#define ENGINE_VALUE_INTERFACE = "ca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue"

#define IS_ENGINE_FUNCTION(t) t == ENGINE_JAVA_LAMBDA_FUNCTION || t == ENGINE_JAVA_REFLECT_FUNCTION || t == ENGINE_LUA_FUNCTION

typedef void* engine_luafunc // stub

typedef struct {
	uint8_t args;
	uint8_t ret;
} engine_jlambda_info;

typedef struct {
	jmethodID id;
	jobject obj_inst;
	ffi_closure* closure;
	ffi_cif* cif;
	uint8_t ret;
	uint8_t args;
	// dereferenced when called to get the current env pointer
	JNIEnv** runtime_env;
} engine_jfuncwrapper;

typedef struct {
	lua_State* state;
	uint8_t closed;
	engine_jfuncwrapper** wrappers;
	size_t wrappers_amt;
	// assigned (never used) before a chunk is executed in Lua
	JNIEnv* runtime_env;
} engine_inst;


typedef struct {
	jobject global_ref; // NULL if static, global ref
	jmethodID id; // method id
	jclass* classes; // pointer to array of classes (should be held as global refs)
} engine_rfunc;

typedef struct engine_value_ engine_value;

typedef struct engine_value_ {
	union {
		long i; // int, long, short, boolean, or byte
		double d; // float or double
		char* str; // null-terminated string
		jobject obj; // object (userdata) or java function (lambda)
		engine_luafunc func; // lua function
		engine_rfunc rfunc; // reflected java function
		struct array { // array
			struct** engine_value_ values; // pointer to block of memory containing pointers to engine values
			uint32_t length; // length of memory block
		};
	} data;
	uint8_t type;
	engine_inst* inst; // engine instance, we need this to parse and clean the value stack
} engine_value;

// init methods, these should be called at least once before opening an instance.
void engine_value_init(JNIEnv* env);

// create engine_value
void engine_value_create(engine_inst* inst);

engine_value* engine_newvalue(JNIEnv* env, engine_inst* inst);
void engine_clearvalues(JNIEnv* env, engine_inst* inst);
// unwraps the java script value to C engine_value
// NOTHING IS ALLOCATED, it just looks up a pair and finds and existing allocation to the value
engine_value* engine_unwrap(JNIEnv* env, engine_inst* inst, jobject obj);
// set global lua function to value
void engine_setfunc(JNIEnv* env, engine_inst* inst, char* name, size_t name_len, engine_value value);
// wrap lambda to script value
jobject engine_tovalue_lambda(JNIEnv* env, engine_inst* inst, jobject jfunc);
// wrap rfunc to script value
jobject engine_tovalue_reflect(JNIEnv* env, engine_inst* inst, engine_rfunc func);
// get info from lambda function class
engine_jlambda_info engine_getlambdainfo(JNIEnv* env, engine_inst* inst, jclass jfunctype);
// call actual java lambda, with argument checking (array arg is a java array of script values)
jobject engine_call_jlambda(JNIEnv* env, jobject lambda, jobject array_value_args);

// utils

// register global ref to class at memory
void classreg(JNIEnv* env, const char* name, jclass* mem);


#endif // ENGINE_H_
