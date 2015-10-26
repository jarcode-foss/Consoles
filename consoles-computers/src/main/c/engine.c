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

// states/active engines are tracked, and will have the same array index when they need to be looked up
static lua_State** engine_states = 0;
static engine_inst** engine_instances = 0;
static size_t registered_engines = 0;

// we use a single caller interface for wrapping Java -> C -> Lua functions,
// since they all boil down to 'void (*lua_cfunc) (lua_State* state)'
static ffi_cif func_cif;

int engine_lookup(engine_inst* inst) {
	uint8_t valid = 0;
	size_t t;
	for (t = 0; t < registered_engines; t++) {
		if (engine_instances[t] == inst) {
			valid = 1;
			break;
		}
	}
	if (!valid) return -1;
	else return t;
}

int engine_state_lookup(lua_State* state) {
	uint8_t valid = 0;
	size_t t;
	for (t = 0; t < registered_engines; t++) {
		if (engine_states[t] == state) {
			valid = 1;
			break;
		}
	}
	if (!valid) return -1;
	else return t;
}

void engine_remove(size_t t) {
	if (registered_engines == 0) return;
	if (engine_debug)
		printf("\nunregistering engine at index: %d\n", (int) t);
	if (registered_engines == 1) {
		free(engine_states);
		free(engine_instances);
	}
	else {
		size_t newlen = (registered_engines - 1) * sizeof(void*);
		if (t != registered_engines) {
			lua_State* state_ptr = engine_states[t];
			engine_inst* eng_ptr = engine_instances[t];
			size_t chunkamt = registered_engines - (t + 1);
			memmove(state_ptr, ((void*) state_ptr) + 1, chunkamt);
			memmove(eng_ptr, eng_ptr + 1, chunkamt);
		}
		engine_instances = realloc(engine_instances, newlen);
		engine_states = realloc(engine_states, newlen);
	}
	registered_engines--;
}

engine_inst* engine_reg(lua_State* state) {
	engine_inst* instance = malloc(sizeof(engine_inst));
	instance->state = state;
	if (registered_engines == 0) {
		engine_instances = malloc(sizeof(void*));
		engine_states = malloc(sizeof(void*));
	}
	else {
		size_t newlen = (registered_engines + 1) * sizeof(void*);
		engine_instances = realloc(engine_instances, newlen);
		engine_states = realloc(engine_states, newlen);
	}
	engine_instances[registered_engines] = instance;
	engine_states[registered_engines] = state;
	registered_engines++;
	
	return instance;
}

void engine_close(engine_inst* inst) {
	
	// lookup the engine index and remove it from our list of engines
	
	int index = engine_lookup(inst);
	if (index != -1) {
		engine_remove(index);
	}
	
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
		ffi_closure_free(inst->wrappers[t]->closure);
		free(wrappers(inst->wrappers[t]);
	}
	
	free(inst->wrappers);
	inst->wrappers = 0;
	inst->wrappers_amt = 0;
	
	//TODO: cleanup here
	
	free(inst);
}

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
	
	if (!FFI_CLOSURES) {
		fprintf(stderr, "\nFFI_CLOSURES are not supported on this architecture (libffi)\n");
		exit(-1);
	}
	
	// ffi function args
	ffi_type* args[1];
	args[0] = &ffi_type_pointer;
	// prepare caller interface
	if (ffi_prep_cif(&func_cif, FFI_DEFAULT_ABI, 1, &ffi_type_sint, args) != FII_OK) {
		fprintf(stderr, "\nfailed to prepare ffi cif for C function wrappers (%s)\n", name);
		exit(-1);
	}
	
	lua_State* state = lua_open();
	luaopen_base(state);
	luaopen_table(state);
	luaopen_io(state);
	luaopen_string(state);
	
	// LuaJIT mode
	if (mode == 1) {
		//TODO: setmode
	}
	
	engine_inst* instance = engine_reg(state);
	instance->state = state;
	instance->env = env;
	instance->closed = 0;
	return (jlong) instance;
}

JNIEXPORT jlong JNICALL Java_jni_LuaEngine_unrestrict(JNIEnv* env, jobject this, jlong ptr) {
	return ptr;
}

JNIEXPORT jint JNICALL Java_jni_LuaEngine_destroyinst(JNIEnv* env, jobject this, jlong ptr) {
	engine_close((engine_inst*) ptr);
	return 0;
}

JNIEXPORT void JNICALL Java_jni_LuaEngine_setdebug(JNIEnv* env, jobject this, jint mode) {
	engine_debug = mode;
}

// this is a (wrapped) function that handles _all_ C->Lua function calls
int engine_handlecall(engine_jfuncwrapper* wrapper, lua_State* state) {
	engine_inst* inst = engine_instances[engine_state_lookup(state)];
	if (wrapper->ret) {
		(env*)->CallObjectMethod(env, wrapper->obj_inst, wrapper->id, ...);
	}
	else {
		
	}
}

// binding function for ffi
int engine_handlecall_binding(ffi_cif* cif, void* ret, void* args[], void* user_data) {
	*(ffi_arg*) ret = engine_handlecall((engine_jfuncwrapper*) user_data, *(lua_State**) args[0]);
}

// magic to turn Java function wrapper (NoArgFunc, TwoArgVoidFunc, etc) into a C function
// and then pushes it onto the lua stack.
void engine_setfunc(engine_inst* inst, char* name, jobject jfunc) {
	
	JNIEnv* env = inst->env;
	
	if (engine_debug) {
		printf("\nwrapping java function from C: %s", name);
	}
	
	// get class
	jclass jfunctype = (*env)->GetObjectClass(env, jfunc);
	
	// obtain argument info
	
	// you might ask "why not just get the method signature?", well that's because reflecting the class
	// and then getting the signature would probably be harder (and slower).
	
	jfieldID fid_return = (*env)->GetStaticFieldID(env, jfunctype, "C_RETURN", "I");
	jfieldID fid_args = (*env)->GetStaticFieldID(env, jfunctype, "C_ARGS", "I");
	jint ret = (*env)->GetStaticIntField(env, jfunctype, fid_return);
	jint args = (*env)->GetStaticIntField(env, jfunctype, fid_args); 
	
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
	
	wrapper->ret = (uint8_t) ret;
	wrapper->args = (uint8_t) args;
	wrapper->cif = cif;
	wrapper->closure = closure;
	wrapper->obj_inst = jfunc;
	wrapper->id = mid;
	
	lua_pushcfunction(inst->state, (lua_CFunction) func_binding);
	lua_setglobal(inst->state, name);
}
