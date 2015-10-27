
#include <jni.h>

#include <lua.h>

#include <stdint.h>

#include <ffi.h>

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
// unqiue globals value
#define ENGINE_LUA_GLOBALS = 9;
// null value
#define ENGINE_NULL = 10;

// this macro does not replace _all_ instances of the class string.
#define ENGINE_CLASS = "java/lang/Class"
#define METHOD_CLASS = "java/lang/reflect/Method"

// project-specific type classpaths. Any type that is part of the Java SE library is allowed to be used
// _without_ a macro, because it is assumed they will not change.

// if you are refactoring the code and these paths change, please update them and recompile the JNI library.
#define ENGINE_LUA_CLASS "ca/jarcode/consoles/computer/interpreter/Lua"
#define ENGINE_ERR_CLASS = "ca/jarcode/consoles/computer/interpreter/luanative/LuaNError";
#define ENGINE_VALUE_CLASS = "ca/jarcode/consoles/computer/interpreter/luanative/LuaNScriptValue"
#define ENGINE_VALUE_INTERFACE = "ca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue"

#define IS_ENGINE_FUNCTION(t) t == ENGINE_JAVA_LAMBDA_FUNCTION || t == ENGINE_JAVA_REFLECT_FUNCTION || t == ENGINE_LUA_FUNCTION

typedef void* engine_luafunc; // stub

/*
 * struct for information about a lambda function (usally passed by value)
 */
typedef struct {
	uint8_t args;
	uint8_t ret;
} engine_lambda_info;

/*
 * This is a function wrapper, a type passed to a special function in engine.c that handles all function calls.
 */
typedef struct {
	jobject obj_inst; // lambda, or instance of object that a reflected method is accessing (can be null)
	ffi_closure* closure; // closure
	union data { // type-unqiue data
		struct lambda {
			jmethodID id; // id of lambda method
			uint8_t ret; // >0 if there is a return value
			uint8_t args; // argument count
		}
		jobject reflect_method // Method instance
	}
	uint8_t type; // lambda or reflected
	// dereferenced when called to get the current env pointer
	JNIEnv** runtime_env;
} engine_jfuncwrapper;

/*
 * An instance of this engine, associated with a lua interpreter instance. A new engine instance should
 * be created for every separate lua chunk to be run.
 */
typedef struct {
	lua_State* state;
	uint8_t closed;
	engine_jfuncwrapper** wrappers;
	size_t wrappers_amt;
	// assigned (never used) before a chunk is executed in Lua
	// this is used for function wrappers that need to find the environment they are running in
	JNIEnv* runtime_env;
} engine_inst;


// IF COPIED MAKE SURE TO CREATE NEW GLOBAL REFS

/*
 * reflected java function
 */
typedef struct {
	jobject obj_inst; // NULL if static, global ref
	jobject reflect_method // Method instance, global ref
} engine_rfunc;

/*
 * lambda java function
 */
typedef struct {
	jobject lambda; // lambda instance, global ref
	jobject class_array; // java array of classes (Class[])
} engine_lfunc;

/*
 * a 'middleground' engine value
 */
typedef struct engine_value_ {
	union {
		long i; // int, long, short, boolean, or byte
		double d; // float or double
		char* str; // null-terminated string
		jobject obj; // object (userdata)
		enbgine_lfunc lfunc; // java function (lambda)
		engine_luafunc func; // lua function
		engine_rfunc rfunc; // reflected java function
		struct array { // array
			struct** engine_value_ values; // pointer to block of memory containing pointers to engine values
			uint32_t length; // length of memory block
		};
		struct lglobals {
			char* name;
			lua_State** runtime_state; // pointer to pointer of lua_State at runtime
		}
	} data;
	uint8_t type;
	engine_inst* inst; // engine instance, we need this to parse and clean the value stack
} engine_value;

// init methods, these should be called at least once before opening an instance.
void engine_value_init(JNIEnv* env);

// create engine value
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
engine_lambda_info engine_getlambdainfo(JNIEnv* env, engine_inst* inst, jclass jfunctype);
// call actual java lambda, with argument checking (array arg is a java array of script values)
jobject engine_call_lambda(JNIEnv* env, engine_value* value, jobject array_value_args); // stub

// wrap lua value to script value
// this operates on the value on the top of the lua stack, popping it after
jobject engine_popvalue_lua(JNIEnv* env, engine_inst* inst, lua_State* state); // stub
// pushes a lambda onto the stack as a wrapped C function
void engine_pushlambda(JNIEnv* env, engine_inst* inst, jobject class_array, jobject jfunc);
void engine_pushreflect(JNIEnv* env, engine_inst* inst, jobject reflect_method, jobject obj_inst);

// utils

// register global ref to class at memory
void classreg(JNIEnv* env, const char* name, jclass* mem);

#endif // ENGINE_H_
