
#include <engine.h>
#include <LuaNScriptValue.h>

#include "pair.h"

static pair_map m;

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateObj
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateObj
(JNIEnv* env, jobject this) {
	engine_value* value = (engine_value*) pair_map_second(m, (_jobject*) this);
}
/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateObj
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateObj
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateString
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateString
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateString
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateString
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateLong
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateLong
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateLong
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateLong
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateShort
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateShort
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateShort
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateShort
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateByte
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateByte
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateByte
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateByte
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateInt
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateInt
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateInt
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateInt
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateFloat
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateFloat
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateFloat
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateFloat
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateDouble
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateDouble
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateDouble
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateDouble
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateBoolean
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateBoolean
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateBoolean
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateBoolean
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    isNull
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_isNull
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateArray
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateArray
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateArray
 * Signature: (Ljava/lang/Class;)Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateArray
(JNIEnv* env, jobject this, jclass) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    isFunction
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_isFunction
(JNIEnv* env, jobject this) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    getAsFunction
 * Signature: ()Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptFunction;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_getAsFunction
(JNIEnv* env, jobject this) {
	
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
(JNIEnv* env, jobject this, jobject) {
	
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    call
 * Signature: ()Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_call
(JNIEnv* env, jobject this) {
	
}
