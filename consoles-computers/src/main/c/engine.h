
#include <jni.h>

#include <lua.h>

#include <stdint.h>

#include <ffi.h>

#ifndef ENGINE_H_
#define ENGINE_H_

typedef struct {
	jmethodID id;
	jobject obj_inst;
	ffi_closure* closure;
	ffi_cif* cif;
	uint8_t ret;
	uint8_t args;
} engine_jfuncwrapper;

typedef struct {
	JNIEnv* env;
	lua_State* state;
	uint8_t closed;
	engine_jfuncwrapper** wrappers;
	size_t wrappers_amt;
} engine_inst;

typedef struct {
	
} engine_value;

void engine_setfunc(engine_inst* inst, JNIEnv* env, char* name, size_t name_len, jobject jfunc);

#endif // ENGINE_H_
