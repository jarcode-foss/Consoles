
#include <jni.h>

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <LuaNScriptValue.h>

#include "engine.h"
#include "pair.h"

static pair_map m;

static jclass value_type;
static jmethodID value_constructor;

static jclass class_type;
static jmethodID id_comptype;

static jclass class_lua;
static jmethodID id_translate;

static jclass class_array;
static jmethodID id_newarray;
static jmethodID id_arrayset;

static jclass class_object;

static jclass exclass;

static uint8_t setup = 0;

static jint throw(JNIEnv* env, const char* message) {
	return (*env)->ThrowNew(env, exclass, message);
}

static void handle_null_const(jmethodID v, const char* message) {
	if (v == 0) {
		fprintf(stderr, "\nfailed to find value constructor (%s)\n", message);
		exit(-1);
	}
}

void engine_value_init(JNIEnv* env) {
	if (!setup) {
		classreg(env, ENGINE_ERR_CLASS, &exclass);
		classreg(env, ENGINE_VALUE_CLASS, &value_type);
		classreg(env, ENGINE_CLASS, &class_type);
		classreg(env, ENGINE_LUA_CLASS, &class_lua);
		classreg(env, "java/lang/Object", &class_object);
		classreg(env, "java/lang/reflect/Array", &class_array); // for generic array setting
		value_constructor = (*env)->GetMethodID(env, value_type, "<init>", "()V");
		handle_null_const(value_const, ENGINE_VALUE_CLASS);
		id_comptype = (*env)->GetMethodID(env, class_type, "getComponentType", "()Ljava/lang/Class;");
		id_newarray = (*env)->GetStaticMethodID(env, class_array, "newInstance", "(Ljava/lang/Class;I)Ljava/lang/Object;");
		id_arrayset = (*env)->GetStaticMethodID(env, class_array, "set", "(Ljava/lang/Object;ILjava/lang/Object;)V"
		{
			char buf[128] = {0};
			strcat(buf, "(Ljava/lang/Class;L");
			strcat(buf, ENGINE_VALUE_INTERFACE);
			strcat(buf, ";)Ljava/lang/Object;"
			id_translate = (*env)->GetStaticMethodID(env, class_lua, "translate", buf);
		}
		pair_map_init(env, &m);
		setup = 1;
	}
}

engine_value* engine_newvalue(engine_inst* inst) {
	engine_value* v = malloc(sizeof(engine_value));
	jobject obj = (*env)->NewObject(env, value_type, value_const);
	obj = (*env)->NewGlobalRef(env, object);
	pair_map_append(m, obj, v);
}

static int valuecmp(void* ptr, void* userdata) {
	if (((engine_value*) ptr)->inst == userdata) {
		if (((engine_value*) ptr)->type == ENGINE_ARRAY) {
			free(((engine_value*) ptr)->data->array->values);
		}
		return 1;
	}
	else return 0;
}

void engine_clearvalues(JNIEnv* env, engine_inst* inst) {
	m.rm(env, &m, &valuecmp, inst, &(m.second_pair));
}

engine_value* engine_unwrap(engine_inst* inst, jobject obj) {
	return (engine_value*) pair_map_second(m, (_jobject*) this);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateObj
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateObj
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_JAVA_OBJECT) {
		return value->data->obj;
	}
	else {
		throw(env, "tried to translate value to object");
		return 0;
	}
}
/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateObj
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateObj
(JNIEnv* env, jobject this) {
	return ((engine_value*) m.second(env, m, (_jobject*) this))->type == ENGINE_JAVA_OBJECT;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateString
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateString
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_STRING) {
		return (*env)->NewStringUTF(env, value->data->str);
	}
	else {
		throw(env, "tried to translate value to string");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateString
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateString
(JNIEnv* env, jobject this) {
	return ((engine_value*) m.second(env, m, (_jobject*) this))->type == ENGINE_STRING;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateLong
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateLong
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jlong) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jlong) value->data->i;
	}
	else {
		throw(env, "tried to translate value to long");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateLong
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateLong
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return type == ENGINE_FLOATING || type == ENGINE_INTEGRAL;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateShort
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateShort
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jshort) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jshort) value->data->i;
	}
	else {
		throw(env, "tried to translate value to short");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateShort
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateShort
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return type == ENGINE_FLOATING || type == ENGINE_INTEGRAL;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateByte
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateByte
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jbyte) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jbyte) value->data->i;
	}
	else {
		throw(env, "tried to translate value to byte");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateByte
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateByte
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return type == ENGINE_FLOATING || type == ENGINE_INTEGRAL;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateInt
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateInt
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jint) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jint) value->data->i;
	}
	else {
		throw(env, "tried to translate value to int");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateInt
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateInt
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return type == ENGINE_FLOATING || type == ENGINE_INTEGRAL;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateFloat
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateFloat
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jfloat) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jfloat) value->data->i;
	}
	else {
		throw(env, "tried to translate value to float");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateFloat
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateFloat
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return type == ENGINE_FLOATING || type == ENGINE_INTEGRAL;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateDouble
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateDouble
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jdouble) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jdouble) value->data->i;
	}
	else {
		throw(env, "tried to translate value to double");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateDouble
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateDouble
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return type == ENGINE_FLOATING || type == ENGINE_INTEGRAL;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateBoolean
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateBoolean
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_FLOATING) {
		return (jbyte) value->data->d;
	}
	else if (value->type == ENGINE_INTEGRAL) {
		return (jbyte) value->data->i;
	}
	else {
		throw(env, "tried to translate value to byte");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateBoolean
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateBoolean
(JNIEnv* env, jobject this) {
	return ((engine_value*) m.second(env, m, (_jobject*) this))->type == ENGINE_BOOLEAN;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    isNull
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_isNull
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_BOOLEAN) {
		return (jboolean) value->data->i;
	}
	else {
		throw(env, "tried to translate value to boolean");
		return 0;
	}
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateArray
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateArray
(JNIEnv* env, jobject this) {
	return ((engine_value*) m.second(env, m, (_jobject*) this))->type == ENGINE_ARRAY;
}

// THE FOLLOWING METHOD IS REALLY SLOW
// however, there is no faster way to do it.

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateArray
 * Signature: (Ljava/lang/Class;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateArray
(JNIEnv* env, jobject this, jclass array_type) {
	
	// this implementation of _translating_ (which is actually doing a recursive array copy) uses
	// Array.newInstance and Array.set. Why? Well, it's easier (primitive array types are handled),
	// and the native implementations of these functions are likely doing handling of types just
	// as fast as I would be.
	
	// the alternative would be to use NewObjectArray and SetObjectArrayElement (different methods
	// for primitive array types), which is just unessecary extra code if the JVM already has a
	// native implementation of array type switching for me.
	
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type != ENGINE_ARRAY) {
		throw(env, "tried to translate value to array");
		return 0;
	}
	// get array component type
	jclass comptype = (*env)->CallObjectMethod(env, array_type, id_comptype);
	// create array from type
	jobject array = (*env)->CallStaticObjectMethod(env, class_array, id_newarray, comptype,
		value->data->array->length);
	uint32_t t;
	for (t = 0; t < value->data->array->length; t++) {
		// get engine_value element, and then get the java counterpart
		jobject wrapped_element = (jobject) (_jobject*) m.first(env, m, value->data->array->values[t]);
		// call Lua.translate(type, value) to recursively translate and resolve values
		jobject java_element = (*env)->CallStaticObjectMethod(env, class_lua, id_translate, comptype,
			wrapped_element);
		// call Array.set(array, i, value) to set the value
		(*env)->CallStaticVoidMethod(env, class_array, id_arrayset, array, t, java_element);
	}
	return array;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    isFunction
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_isFunction
(JNIEnv* env, jobject this) {
	uint8_t type = ((engine_value*) m.second(env, m, (_jobject*) this))->type;
	return IS_ENGINE_FUNCTION(type);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    set
 * Signature: (Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_set
(JNIEnv* env, jobject this, jobject, jobject) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    get
 * Signature: (Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_get
(JNIEnv* env, jobject this, jobject script_value) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    call
 * Signature: ()Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_call
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) m.second(env, m, (_jobject*) this);
	if (value->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
		// empty arguments array
		jobjectArray arr = (*env)->NewObjectArray(env, 0, value_type, 0);
		jobject java_object = engine_call_jlambda(env, value->data->obj, arr);
	}
	if (value->type == ENGINE_JAVA_REFLECT_FUNCTION) {
		
	}
	if (value->type == ENGINE_LUA_FUNCTION) {
		// call value->data->func
	}
	else {
		throw(env, "tried to call value as function");
		return 0;
	}
}
/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    call
 * Signature: ([Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_call
(JNIEnv* env, jobject this, jobjectArray arr) {
	
}
