#include <jni.h>
#include <jni_md.h>

#include <lua.h>
#include <luajit.h> 

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <LuaEngine.h>

#include "engine.h"

static int engine_debug = 0;

// states/active engines are tracked, and will have the same array index when they need to be looked up
static lua_State** engine_states = 0;
static engine_inst** engine_instances = 0;
static size_t registered_engines = 0;

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
		if (t != registered_engines) {
			lua_State* state_ptr = engine_states[t];
			engine_inst* eng_ptr = engine_instances[t];
			size_t chunkamt = registered_engines - (t + 1);
			memmove(state_ptr, ((void*) state_ptr) + 1, chunkamt);
			memmove(eng_ptr, eng_ptr + 1, chunkamt);
		}
		else {
			size_t newlen = (registered_engines - 1) * sizeof(void*);
			engine_instances = realloc(engine_instances, newlen);
			engine_states = realloc(engine_states, newlen);
		}
		registered_engines--;
	}
}

engine_inst* engine_reg(lua_State* state) {
	engine_inst* instance = malloc(sizeof(engine_inst));
	instance->state = state;
	if (registered_engines = 0) {
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
	int index = engine_lookup(inst);
	if (index != -1) {
		engine_remove(index);
	}
	if (inst->closed) return;
	
	//TODO: cleanup here
	
	free(inst);
}

JNIEXPORT jlong JNICALL Java_jni_LuaEngine_setupinst(JNIEnv* env, jobject this, jint mode, jlong ptr) {
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

void engine_handlecall(lua_State* state) {
	engine_inst* inst = engine_instances[engine_state_lookup(state)];
	
}

void engine_setfunc(engine_inst* inst, JNIEnv* env, char* name, size_t name_len, jobject jfunc) {
	
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
	for (i = 0; i < args; i++) {
		strcat(buf, "Ljava/lang/Object;");
	}
	if (ret) {
		strcat(buf, ")Ljava/lang/Object;");
	}
	else {
		strcat(buf, ")V");
	}
	
	jmethodID mid = (*env)->GetMethodID(env, jfunctype, "call", buf);
}
