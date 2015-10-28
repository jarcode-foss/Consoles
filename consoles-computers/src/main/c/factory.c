
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

// boring mapping
engine_value* engine_popvalue_lua(JNIEnv* env, engine_inst* inst, lua_State* state) {
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
		// this is a stub (this translates to java objects being impossible to return to java code)
		lua_pop(state, 1);
	}
	else if (lua_isuserdata(state, -1)) {
		// this is a stub (see above)
		lua_pop(state, 1);
	}
	else if (lua_isfunction(state, -1)) {
		
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

void engine_pushvalue_lua(JNIEnv* env, engine_inst* inst, lua_State* state, engine_value* value) {
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
			engine_pushvalue_lua(env, inst, state, value->data->array->values[i]);
			// set table entry
			lua_settable(state, -3);
			// pop key and value
			lua_pop(state, 2);
		}
	}
	else if (value->type == ENGINE_JAVA_OBJECT) {
		// some serious ass magic goes here
		
	}
	else if (value->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
		// more magic
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
(JNIEnv* env, jobject this, jobjectArray, jobject) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNFunctionFactory
 * Method:    createFunction
 * Signature: (Ljava/lang/reflect/Method;Ljava/lang/Object;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptFunction;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNFunctionFactory_createFunction__Ljava_lang_reflect_Method_2Ljava_lang_Object_2
(JNIEnv* env, jobject this, jobject, jobject) {
	
}

  /*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (Z)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__Z
(JNIEnv* env, jobject this, jboolean) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (F)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__F
(JNIEnv* env, jobject this, jfloat) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (D)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__D
(JNIEnv* env, jobject this, jdouble) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (Ljava/lang/String;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__Ljava_lang_String_2
(JNIEnv* env, jobject this, jstring) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (I)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__I
(JNIEnv* env, jobject this, jint) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (J)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__J
(JNIEnv* env, jobject this, jlong) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (S)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__S
(JNIEnv* env, jobject this, jshort) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (B)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__B
(JNIEnv* env, jobject this, jbyte) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translate
 * Signature: (C)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translate__C
(JNIEnv* env, jobject this, jchar) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    list
 * Signature: ([Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_list
(JNIEnv* env, jobject this, jobjectArray) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    nullValue
 * Signature: ()Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_nullValue
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory
 * Method:    translateObj
 * Signature: (Ljava/lang/Object;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNValueFactory_translateObj
(JNIEnv* env, jobject this, jobject) {
	
}
