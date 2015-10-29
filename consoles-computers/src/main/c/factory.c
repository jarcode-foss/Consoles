
#include <jni.h>

#include <lua.h>
#include <luaxlib.h>

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <limits.h>

#include <LuaNScriptValue.h>

#include "engine.h"
#include "pair.h"

// this one is really complicated
// instead of using the standard registry, we use our own table (so we can index things with numbers)
// we do a dual association when registering new functions, after attempting to look the function up
// we then pass the id to the engine value, which can be later looked up to call the function

// this also pops a value
void engine_handleregistry(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* v) {
		v->type = ENGINE_LUA_FUNCTION;
		
		// make copy of function, and put it on the top of the stack
		lua_pushvalue(state, -1);
		// push registry on the stack
		lua_getglobal(state, FUNCTION_REGISTRY);
		// swap registry and lua function, so that the function is on top
		engine_swap(state, -1, -2);
		// swap function (top) with value from function table
		// (result should be nil or a number)
		lua_gettable(state, -2);
		if (lua_isnil(state, -1)) { // no function mapped
			// pop the nil value
			lua_pop(state, -1);
			// (we are now back to the original function at the top)
			// increment and push new function index
			function_index++:
			lua_pushinteger(state, function_index);
			// (we now have (function, key) pair)
			// push second copy of key for second association
			lua_pushinteger(state, function_index);
			// copy function to top of stack
			lua_pushvalue(state, -3);
			// push association (function, id)
			lua_settable(state, -5);
			// push association (id, function)
			lua_settable(state, -3);
			// pop registry
			lua_pop(state, -1);
			
			v->data->func = function_index;
		}
		else { // if there is a function mapped already
			v->data->func = (uint32_t) lua_tonumber(state, -1);
			// pop registry and id
			lua_pop(state, 2);
		}
}
// boring mapping
engine_value* engine_popvalue(JNIEnv* env, engine_inst* inst, lua_State* state) {
	engine_value* v = engine_newvalue(env, inst);
	if (lua_isnumber(state, -1)) {
		v->type = ENGINE_FLOATING;
		v->data->d = (double) lua_tonumber(state, -1);
	}
	else if (lua_isboolean(state, -1)) {
		v->type = ENGINE_BOOLEAN;
		v->type->i = (long) lua_toboolean(state, -1);
	}
	else if (lua_isstring(state, -1)) {
		v->type = ENGINE_STRING;
		// strings in lua are retarded. They are null terminated but can have nulls
		// in the middle of the string (wat)
		const char* lstring = lua_tostring(state, -1);
		size_t len = lua_strlen(state, -1);
		char* s = malloc(sizeof(char) * (len + 1));
		memcpy(s, lstring, len);
		s[len] = '\0';
		
		// this is a (bad) temporary fix if lua tries to pass null characters to C
		// I can't think of a case where I would actually need to recieve null characters
		// on the java size, though, so I'm leaving it like this.
		size_t t;
		for (t = 0; t < len; t++) {
			if (s[t] == '\0')
				s[t] = '?';
		}
		
		v->data->str = s;
	}
	else if (lua_isnoneornil(state, -1)) {
		lua_pop(state, 1);
	}
	else if (lua_islightuserdata(state, -1)) {
		// this is a stub (we don't use lightuserdata)
		lua_pop(state, 1);
	}
	else if (lua_isuserdata(state, -1)) {
		v->type = ENGINE_JAVA_OBJECT;
		// get userdata
		engine_userdata* d = (engine_userdata*) luaL_checkudata(state, -1, "Engine.userdata");
		v->data->obj = (*env)->NewGlobalRef(env, d->obj);
		// pop userdata
		lua_pop(state, 1);
	}
	else if (lua_isfunction(state, -1)) {
		engine_handleregistry(env, inst, state, v);
	}
	else if (lua_iscfunction(state, -1)) {
		// this is a stub, however, there is no reason for java (or C) to be spitting a function at lua
		// and then getting the same function back.
		lua_pop(state, 1);
	}
	// threads should _not_ be happening
	// if we run into this, scream at stderr and return null
	else if (lua_isthread(state, -1)) {
		fprintf(stderr, "\ntried to convert thread value from native lua engine (wat)\n");
	}
	
	// standard behaviour from the script layer is to convert into an array
	else if (lua_istable(state, -1)) {
		
		// if this happens, that means there was like 20 tables nested in each other.
		// return null if the user is being retarded
		if (lua_gettop(state) == LUA_MINSTACK - 1) {
			lua_pop(state, 1);
			return v;
		}
		
		v->type = ENGINE_ARRAY;
		unsigned short t = 0;
		while (1) {
			if (t == USHRT_MAX) break;
			lua_pushinteger(state, t);
			lua_gettable(state, -2);
			if (lua_isnil(state, -1)) {
				lua_pop(state, 1);
				t++;
				break;
			}
			lua_pop(state, 1);
			t++;
		}
		v->data->array->length = t;
		v->data->array->values = malloc(sizeof(engine_value**) * t);
		unsigned short i;
		for (i = 0; i < t; i++) {
			// push key
			lua_pushinteger(state, i);
			// swap key with value (of some sort)
			lua_gettable(state, -2);
			// recurrrrrssssiiiiioooon (and popping the value)
			v->data->array->values[i] = engine_popvalue_lua(env, inst, state);
		}
		lua_pop(state, 1);
	}
	return v;
}

void engine_pushvalue(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* value) {
	if (value->type == ENGINE_BOOLEAN) {
		lua_pushboolean(state, (int) value->data->i);
	}
	else if (value->type == ENGINE_FLOATING) {
		lua_pushnumber(state, value->data->d);
	}
	else if (value->type == ENGINE_INTEGRAL) {
		lua_pushnumber(state, (double) value->data->i);
	}
	else if (value->type == ENGINE_STRING) {
		lua_pushstring(state, type->data->str);
	}
	else if (value->type == ENGINE_ARRAY) {
		
		// overflow (well, not really, but it shouldn't be getting this big)
		if (lua_gettop == LUA_MINSTACK - 1) {
			lua_pushnil(state);
			return;
		}
		
		// create and push new table
		lua_newtable(state);
		unsigned short i;
		for (i = 0; i < value->data->array->length; i++) {
			// push key
			lua_pushinteger(state, i);
			// push value
			if (value->data->array->values[i]) {
				engine_pushvalue_lua(env, inst, state, value->data->array->values[i]);
			}
			else {
				lua_pushnil(state);
			}
			// set table entry
			lua_settable(state, -3);
			// pop key and value
			lua_pop(state, 2);
		}
	}
	else if (value->type == ENGINE_JAVA_OBJECT) {
		// allocate new userdata (managed by lua)
		engine_userdata* userdata = lua_newuserdata(state, sizeof(engine_userdata));
		// create new global ref
		userdata->obj = (*env)->NewGlobalRef(value->data->obj);
		// set env pointer to pointer
		userdata->runtime_env = &(inst->runtime_env);
		userdata->released = 0;
		// register floating reference
		engine_addfloating(inst, userdata->obj);
		// get our special metatable
		luaL_getmetatable(state, "Engine.userdata");
		// set metatable to our userdatum
		// it pops itself off the stack and assigns itself to index -2
		lua_setmetatable(state, -2);
	}
	else if (value->type == ENGINE_LUA_GLOBALS) {
		// stub
		lua_pushnil(state);
	}
	else if (value->type == ENGINE_LUA_FUNCTION) {
		// stub
		lua_pushnil(state);
	}
	else if (value->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
		// magic
		engine_pushlambda(env, inst, value->lfunc->lambda, value->lfunc->class_array);
	}
	else if (value->type == ENGINE_JAVA_REFLECT_FUNCTION) {
		// you're a wizard, harry
		engine_pushreflect(env, inst, value->rfunc->reflect_method, value->rfunc->obj_inst);
	}
	else if (value->type == ENGINE_NULL) {
		lua_pushnil(state);
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNFunctionFactory
 * Method:    createFunction
 * Signature: ([Ljava/lang/Class;Ljava/lang/Object;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptFunction;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNFunctionFactory_createFunction___3Ljava_lang_Class_2Ljava_lang_Object_2
(JNIEnv* env, jobject this, jobjectArray class_array, jobject lambda) {
	engine_value* value = engine_newsharedvalue(env);
	value->type == ENGINE_JAVA_LAMBDA_FUNCTION;
	value->data->lfunc->class_array = (*env)->NewGlobalRef(env, class_array);
	value->data->lfunc->lambda = (*env)->NewGlobalRef(env, lambda);
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNFunctionFactory
 * Method:    createFunction
 * Signature: (Ljava/lang/reflect/Method;Ljava/lang/Object;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptFunction;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNFunctionFactory_createFunction__Ljava_lang_reflect_Method_2Ljava_lang_Object_2
(JNIEnv* env, jobject this, jobject reflect_method, jobject obj_inst) {
	engine_value* value = engine_newsharedvalue(env);
	value->type = ENGINE_JAVA_REFLECT_FUNCTION;
	value->data->rfunc->reflect_method = (*env)->NewGlobalRef(env, reflect_method);
	value->data->rfunc->obj_inst = (*env)->NewGlobalRef(env, obj_inst);
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (ZLca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__ZLca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jboolean boolean, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_BOOLEAN;
	value->data->i = boolean;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (FLca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__FLca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jfloat f, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_FLOATING;
	value->data->d = (double) f;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (DLca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__DLca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jdouble d, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_FLOATING;
	value->data->d = d;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (Ljava/lang/String;Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__Ljava_lang_String_2Lca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jstring str, jobject) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	const char* characters = (*env)->GetStringUTFChars(env, str, 0);
	value->type = ENGINE_STRING;
	size_t len = strlen(characters);
	value->data->str = malloc(sizeof(char) * len);
	memmove(value->data->str, characters, len);
	(*env)->ReleaseStringUTFChars(env, str, characters);
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (ILca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__ILca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jint i, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_INTEGRAL;
	value->data->i = i;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (JLca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__JLca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jlong l, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_INTEGRAL;
	value->data->i = l;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (SLca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__SLca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jshort s, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_INTEGRAL;
	value->data->i = s;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (BLca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__BLca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jbyte b, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_INTEGRAL;
	value->data->i = b;
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    list
 * Signature: ([Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_list
(JNIEnv* env, jobject this, jobjectArray elements, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_ARRAY;
	jsize len = (*env)->GetArrayLength(env, elements);
	value->data->array->values = malloc(sizeof(engine_inst*));
	value->data->array->length = len;
	int t;
	for (t = 0; t < len; t++) {
		jobject element = (*env)->GetObjectArrayElement(env, elements, t);
		engine_inst* element_value = m.native(env, m, element);
		if (element_value) {
			value->data->array->values[t] = element_value;
		}
		else value->data->array->values[t] = 0;
	}
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    nullValue
 * Signature: (Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_nullValue
(JNIEnv* env, jobject this, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	return engine_wrap(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translateObj
 * Signature: (Ljava/lang/Object;Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translateObj
(JNIEnv* env, jobject this, jobject obj, jobject jglobals) {
	if (!jglobals) {
		throw(env, "tried to translate with null globals");
		return 0;
	}
	engine_value* globals = engine_unwrap(env, jglobals);
	engine_inst* inst = globals->inst;
	engine_value* value = engine_newvalue(env, inst);
	value->type = ENGINE_JAVA_OBJECT;
	value->data->obj = (*env)->NewGlobalRef(env, obj);
	return engine_wrap(env, value);
}
