#include <jni.h>
#include <jni_md.h>

#include <lua.h>
#include <luajit.h> 

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <ffi.h>

#include <LuaEngine.h>

#include "engine.h"

static int engine_debug = 0;
static uint8_t setup = 0;

static jclass class_method;

static jmethodID id_methodcall;

static jmethodID id_methodresolve;
static jmethodID id_methodid;

static jmethodID id_hashcode;

// we use a single caller interface for wrapping Java -> C -> Lua functions,
// since they all boil down to 'void (*lua_cfunc) (lua_State* state)'
static ffi_cif func_cif;

void engine_close(JNIEnv* env, engine_inst* inst) {
	
	// if the engine was already marked closed, do nothing
	if (inst->closed) return;
	
	// free all function wrappers (and underlying ffi closures).
	// the amount of closures registered will continue to grow as
	// java functions are registered, and will only be free'd when
	// the entire engine is closed.
	
	// this could be considered a memory leak, so if it has a noticable
	// impact over the life of the Lua VM, I will find a way to interact
	// with the Lua GC so that I can free wrappers as their lua components
	// are free'd.
	int t;
	for (t = 0; t < inst->wrappers_amt; t++) {
		// free closure
		ffi_closure_free(inst->wrappers[t]->closure);
		
		// delete global reference to java function
		if (inst->wrappers[t]->obj_inst) { // if it's null, it was for a reflected static function
			(*env)->DeleteGlobalRef(env, inst->wrappers[t]->obj_inst);
		}
		
		// reflected methods have a Method instance to be deleted
		if (inst->wrappers[t]->type == ENGINE_JAVA_REFLECT_FUNCTION) {
			(*env)->DeleteGlobalRef(env, inst->wrappers[t]->data->reflect->method);
		}
		
		free(wrappers(inst->wrappers[t]);
	}
	
	free(inst->wrappers);
	inst->wrappers = 0;
	inst->wrappers_amt = 0;
	
	//TODO: cleanup here
	
	free(inst);
}

// unused, probably should be used
void engine_removewrapper(engine_inst* inst, engine_jfuncwrapper* wrapper) {
	if (inst->wrappers_amt == 0) return;
	if (inst->wrappers_amt == 1) {
		free(inst->wrappers);
	}
	else {
		uint8_t valid = 0;
		int t;
		for (t = 0; t < inst->wrappers_amt; t++) {
			if (wrapper == inst->wrappers[t]) {
				valid = 1;
				break;
			}
		}
		if (!valid) return;
		if (t != inst->wrappers_amt) {
			void* ptr = inst->wrappers[t];
			memmove(ptr, ptr + 1, inst->wrappers_amt - (t + 1));
		}
		inst->wrappers = realloc(inst->wrappers, (inst->wrappers_amt - 1) * sizeof(void*));
	}
	inst->wrappers_amt--;
}

void engine_regwrapper(engine_inst* inst, engine_jfuncwrapper* wrapper) {
	if (inst->wrappers_amt == 0) {
		inst->wrappers = malloc(sizeof(void*));
	}
	else {
		inst->wrappers = realloc(inst->wrappers, (inst->wrappers_amt + 1) * sizeof(void*));
	}
	inst->wrappers[inst->wrappers_amt] = wrapper;
	inst->wrappers_amt++;
}

JNIEXPORT jlong JNICALL Java_jni_LuaEngine_setupinst(JNIEnv* env, jobject this, jint mode, jlong ptr) {
	
	if (!setup) {
		if (!FFI_CLOSURES) {
			fprintf(stderr, "\nFFI_CLOSURES are not supported on this architecture (libffi)\n");
			exit(-1);
		}
		
		// ffi function args
		ffi_type* args[1];
		args[0] = &ffi_type_pointer;
		// prepare caller interface
		if (ffi_prep_cif(&func_cif, FFI_DEFAULT_ABI, 1, &ffi_type_sint, args) != FII_OK) {
			fprintf(stderr, "\nfailed to prepare ffi caller interface for C function wrappers (%s)\n", name);
			exit(-1);
		}
		
		classreg(env, "java/lang/Object", &class_object);
		classreg(env, ENGINE_CLASS, &class_type);
		classreg(env, ENGINE_LUA_CLASS, &class_lua);
		id_methodresolve = (*env)->GetStaticMethodID(env, class_lua, "resolveMethod", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;");
		id_hashcode = (*env)->GetMethodID(env, class_object, "hashCode", "()I");
		id_methodid = (*env)->GetStaticMethodID(env, class_lua, "methodId", "(Ljava/lang/reflect/Method;)J");
		engine_value_init(env);
		
		// Method class
		classreg(env, "java/lang/reflect/Method", &class_method);
		id_methodcall = (*env)->GetMethodID(env, class_method, "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");
		
		setup = 1;
	}
	
	lua_State* state = lua_open();
	luaopen_base(state);
	luaopen_table(state);
	luaopen_io(state);
	luaopen_string(state);
	
	// register generic userdata table (for java objects)
	luaL_newmetatable(state, "Engine.userdata");
	// we are setting the special __index key, which lua calls every time it tries to index an object
	lua_pushstring(state, "__index");
	// attach our function to handle generic calls into an object
	lua_pushcfunction(state, &engine_handleobjcall);
	// set key and value
	lua_settable(state, -3);
	// pop key, value, and metatable off the stack
	lua_pop(state, 3);
	
	// LuaJIT mode
	if (mode == 1) {
		//TODO: setmode
	}
	
	engine_inst* instance = malloc(sizeof(engine_inst));
	instance->state = state;
	instance->env = env;
	instance->closed = 0;
	
	return (jlong) instance;
}

JNIEXPORT jlong JNICALL Java_jni_LuaEngine_unrestrict(JNIEnv* env, jobject this, jlong ptr) {
	return ptr;
}

JNIEXPORT jint JNICALL Java_jni_LuaEngine_destroyinst(JNIEnv* env, jobject this, jlong ptr) {
	engine_close(env, (engine_inst*) ptr);
	return 0;
}

JNIEXPORT void JNICALL Java_jni_LuaEngine_setdebug(JNIEnv* env, jobject this, jint mode) {
	engine_debug = mode;
}

// this is a function that handles calls on userdata
// this can be optimized, but to improve speed, all types passed to this layer
// would have to be mapped out (and cleaned up), instead of doing it on the fly.
int engine_handleobjcall(lua_State* state) {
	// first arg should be userdata
	engine_userdata* d = (engine_userdata*) luaL_checkudata(state, 1, "Engine.userdata");
	// second arg should be string, indexing a java object with anything else makes no sense
	char* str = luaL_checkstring(state, 2);
	jobject obj = d->obj;
	JNIEnv* env = d->engine->runtime_env;
	jstring jstr = (*env)->NewStringUTF(env, str);
	jobject method = (*env)->CallStaticObjectMethod(obj, class_lua, id_methodresolve, obj, jstr);
	engine_pushreflect(env, d->engine, method, obj);
	return 1;
}

// this is a (wrapped) function that handles _all_ C->Lua function calls
int engine_handlecall(engine_jfuncwrapper* wrapper, lua_State* state) {
	JNIEnv* env = wrappers->engine->runtime_env;
	if (wrapper->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
		if (wrapper->data->lambda->ret) {
			(*env)->CallVoidMethodV(env, wrapper->obj_inst, wrapper->data->lambda->id, __);
		}
		else {
			(*env)->CallVoidMethodV(env, wrapper->obj_inst, wrapper->data->lambda->id, __);
		}
	}
	else if (wrapper->type == ENGINE_JAVA_REFLECT_FUNCTION) {
		(*env)->CallObjectMethodV(env, wrapper->data->reflect->method, id_methodcall, __)
	}
}

// binding function for ffi
int engine_handlecall_binding(ffi_cif* cif, void* ret, void* args[], void* user_data) {
	*(ffi_arg*) ret = engine_handlecall((engine_jfuncwrapper*) user_data, *(lua_State**) args[0]);
}

engine_lambda_info engine_getlambdainfo(JNIEnv* env, engine_inst* inst, jclass jfunctype) {
	jfieldID fid_return = (*env)->GetStaticFieldID(env, jfunctype, "C_RETURN", "I");
	jfieldID fid_args = (*env)->GetStaticFieldID(env, jfunctype, "C_ARGS", "I");
	jint ret = (*env)->GetStaticIntField(env, jfunctype, fid_return);
	jint args = (*env)->GetStaticIntField(env, jfunctype, fid_args); 
	engine_lambda_info info = {.ret = ret, .args = args};
	return info;
}

// magic to turn Java lambda function wrapper (NoArgFunc, TwoArgVoidFunc, etc) into a C function
// and then pushes it onto the lua stack.
void engine_pushlambda(JNIEnv* env, engine_inst* inst, jobject jfunc, jobject class_array) {
	if (engine_debug) {
		printf("\nwrapping java lambda function from C: %s", name);
	}
	
	// get class
	jclass jfunctype = (*env)->GetObjectClass(env, jfunc);
	
	// obtain func (lambda) info
	uint8_t ret, args;
	{
		engine_lambda_info info = engine_getlambdainfo(env, inst, jfunctype);
		ret = info.ret;
		args = info.args;
	}
	
	// obtain argument info
	
	// you might ask "why not just get the method signature?", well that's because reflecting the class
	// and then getting the signature would probably be harder (and slower).
	
	// build signature and get method
	char buf[128] = {0};
	strcat(buf, "(");
	size_t i;
	for (i = 0; i < args; i++)
		strcat(buf, "Ljava/lang/Object;");
	if (ret) strcat(buf, ")Ljava/lang/Object;");
	else strcat(buf, ")V");
	
	jmethodID mid = (*env)->GetMethodID(env, jfunctype, "call", buf);
	void *func_binding; // our function pointer
	ffi_closure* closure = ffi_closure_alloc(sizeof(ffi_closure), &func_binding); // ffi closure
	
	// this shouldn't happen
	if (!closure) {
		fprintf(stderr, "\nfailed to allocate ffi closure (%s)\n", name);
		exit(-1);
	}
	
	engine_jfuncwrapper* wrapper = malloc(sizeof(engine_jfuncwrapper));
	engine_regwrapper(inst, wrapper);
	
	if (ffi_prep_closure_loc(closure, &func_cif, &engine_handlecall_binding, wrapper, func_binding) != FFI_OK) {
		fprintf(stderr, "\nfailed to prepare ffi closure (%s)\n", name);
		exit(-1);
	}
	
	wrapper->closure = closure;
	wrapper->type == ENGINE_JAVA_LAMBDA_FUNCTION;
	wrapper->data->lambda->ret = (uint8_t) ret;
	wrapper->data->lambda->args = (uint8_t) args;
	wrapper->data->lambda->id = mid;
	wrapper->obj_inst = (*env)->NewGlobalRef(env, jfunc);
	wrapper->runtime_env = &(inst->runtime_env);
	
	lua_pushcfunction(inst->state, (lua_CFunction) func_binding);
}

// same idea as above, but with reflection types instead (Method). We also do a lookup in
// this implementation to find methods that have already been wrapped to consverve memory over
// the lifetime of a lua VM/interpreter.
void engine_pushreflect(JNIEnv* env, engine_inst* inst, jobject reflect_method, jobject obj_inst) {
	if (engine_debug) {
		printf("\nwrapping java reflect function from C: %s", name);
	}
	
	// compute method id
	long id = (*env)->CallStaticLongMethod(env, class_lua, id_methodid, reflect_method);
	// search for reflect wrapper with equal id
	size_t t;
	for(t = 0; t < inst->wrappers_amt; t++) {
		engine_jfuncwrapper wrapper = inst->wrappers[t];
		if (wrapper->type == ENGINE_JAVA_REFLECT_FUNCTION && wrapper->reflect->id == id) {
			// found identical wrapper, recycle it and return;
			lua_pushcfunction(inst->state, wrapper->func);
			return;
		}
	}
	
	void *func_binding; // our function pointer
	ffi_closure* closure = ffi_closure_alloc(sizeof(ffi_closure), &func_binding); // ffi closure
	
	// this shouldn't happen
	if (!closure) {
		fprintf(stderr, "\nfailed to allocate ffi closure (%s)\n", name);
		exit(-1);
	}
	
	engine_jfuncwrapper* wrapper = malloc(sizeof(engine_jfuncwrapper));
	engine_regwrapper(inst, wrapper);
	
	if (ffi_prep_closure_loc(closure, &func_cif, &engine_handlecall_binding, wrapper, func_binding) != FFI_OK) {
		fprintf(stderr, "\nfailed to prepare ffi closure (%s)\n", name);
		exit(-1);
	}
	
	wrapper->closure = closure;
	wrapper->type = ENGINE_JAVA_REFLECT_FUNCTION;
	wrapper->data->reflect->method = (*env)->NewGlobalRef(env, reflect_method);
	wrapper->obj_inst = (*env)->NewGlobalRef(env, obj_inst);
	wrapper->runtime_env = &(inst->runtime_env);
	wrapper->func = (lua_CFunction) func_binding;
	
	lua_pushcfunction(inst->state, (lua_CFunction) func_binding);
}
