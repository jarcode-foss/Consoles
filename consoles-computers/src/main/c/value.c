
#include <jni.h>

#include <lua.h>

#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <LuaNScriptValue.h>

#include "engine.h"
#include "pair.h"

/*
 * 
 * This unit is a native implementation of LuaNScriptValue. It serves as a buffer
 * for Java to interact with Lua values, by storing everything in a C struct.
 * 
 * Lua and Java values are mapped to C, making the data extremely easy to work with.
 * 
 * The downside of this approach is that this middle ground value is allocated and
 * never free'd until the lua interpreter dies. I try to improve this using Java's
 * PhantomReference<T>'s class to track when the Java object is unreachable,
 * however that depends on how JNI's global references behaves with objects that
 * have gone out-of-scope.
 * 
 * If the about is possible, a function exposed to Java called engine_freehandles
 * will be created to allow parsing of the entire value stack, while looking for
 * marked values that are ready to be collected.
 * 
 * Written by Levi Webb (Jarcode)
 * 
 */

static jclass value_type;
static jmethodID value_constructor;

// class 'Array'
static jclass class_array;
static jmethodID id_newarray;
static jmethodID id_arrayset;

static uint8_t setup = 0;

static inline void handle_null_const(jmethodID v, const char* message) {
    if (v == 0) {
        fprintf(stderr, "\nC: failed to find value constructor (%s)\n", message);
        engine_abort();
    }
}

static inline engine_value* findnative(JNIEnv* env, jobject ref) {
    engine_value* value = pair_map_native(ENGINE_SCRIPT_VALUE_ID, ref, env);
    if (value) {
        return value;
    }
    else {
        throw(env, "C: value has been released");
        return 0;
    }
}

void setup_value(JNIEnv* env, jmp_buf handle) {
    if (!setup) {
        classreg(env, ENGINE_VALUE_CLASS, &value_type, handle);
        classreg(env, "java/lang/reflect/Array", &class_array, handle); // for generic array setting
        value_constructor = (*env)->GetMethodID(env, value_type, "<init>", "()V");
        CHECKEX(env, handle);
        handle_null_const(value_constructor, ENGINE_VALUE_CLASS);
        id_newarray = (*env)->GetStaticMethodID(env, class_array, "newInstance", "(Ljava/lang/Class;I)Ljava/lang/Object;");
        CHECKEX(env, handle);
        id_arrayset = (*env)->GetStaticMethodID(env, class_array, "set", "(Ljava/lang/Object;ILjava/lang/Object;)V");
        CHECKEX(env, handle);
        
        pair_map_init(ENGINE_SCRIPT_VALUE_ID);
        setup = 1;
    }
}

engine_value* engine_newvalue(JNIEnv* env, engine_inst* inst) {
    engine_value* v = engine_newsharedvalue(env);
    // associate this value with an instance
    v->inst = inst;
    return v;
}

engine_value* engine_newsharedvalue(JNIEnv* env) {
    engine_value* v = malloc(sizeof(engine_value));
    
#if ENGINE_CDEBUG > 0 // initialize debug signature, if needed
    v->DEBUG_SIGNATURE = ENGINE_DEBUG_SIGNATURE;
#endif // ENGINE_CDEBUG
    v->inst = 0;
    
    jobject obj = (*env)->NewObject(env, value_type, value_constructor);
    pair_map_append(ENGINE_SCRIPT_VALUE_ID, obj, v, env);
    (*env)->DeleteLocalRef(env, obj);
    return v;
}

static void valuefree(JNIEnv* env, engine_value* value) {
    if (value->type == ENGINE_ARRAY) {
        free(value->data.array.values);
    }
    else if (value->type == ENGINE_STRING) {
        // strings in our values are either copied from java or lua,
        // we need to free the string when we parse the stack
        free(value->data.str);
    }
    // clear global references to reflection method
    else if (value->type == ENGINE_JAVA_REFLECT_FUNCTION) {
        (*env)->DeleteGlobalRef(env, value->data.rfunc.obj_inst);
        (*env)->DeleteGlobalRef(env, value->data.rfunc.reflect_method);
    }
    // clear global references to lambda method and argument class array
    else if (value->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
        (*env)->DeleteGlobalRef(env, value->data.lfunc.lambda);
        (*env)->DeleteGlobalRef(env, value->data.lfunc.class_array);
    }
    // clear global references to object
    else if (value->type == ENGINE_JAVA_OBJECT) {
        (*env)->DeleteGlobalRef(env, value->data.obj);
    }
    free(value);
}

void engine_releasevalue(JNIEnv* env, engine_value* value) {
    pair_map_rm_native(ENGINE_SCRIPT_VALUE_ID, value, env);
    valuefree(env, value);
}

static int valueparse(void* ptr, void* userdata) {
    if (((engine_value*) ptr)->inst && ((engine_value*) ptr)->inst == userdata) {
        valuefree(((engine_value*) ptr)->inst->runtime_env, (engine_value*) ptr);
        return 1;
    }
    else return 0;
}

void engine_clearvalues(JNIEnv* env, engine_inst* inst) {
    pair_map_rm_context(ENGINE_SCRIPT_VALUE_ID, &valueparse, inst);
}

inline engine_value* engine_unwrap(JNIEnv* env, jobject obj) {
    return findnative(env, obj);
}

inline jobject engine_wrap(JNIEnv* env, engine_value* value) {
    return pair_map_java(ENGINE_SCRIPT_VALUE_ID, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    release
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_release
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (value) engine_releasevalue(env, value);
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    copy
 * Signature: ()Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_copy
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    engine_value* copy = engine_newvalue(env, value->inst);
    copy->type = value->type;
    switch (value->type) {
        case ENGINE_ARRAY: {
            size_t len = value->data.array.length;
            copy->data.array.values = malloc(sizeof(engine_value*) * len);
            memcpy(copy->data.array.values, value->data.array.values, len);
            break;
        }
        case ENGINE_JAVA_LAMBDA_FUNCTION:
            copy->data.lfunc.lambda = (*env)->NewGlobalRef(env, value->data.lfunc.lambda);
            copy->data.lfunc.class_array = (*env)->NewGlobalRef(env, value->data.lfunc.class_array);
            break;
        case ENGINE_JAVA_REFLECT_FUNCTION:
            copy->data.rfunc.reflect_method = (*env)->NewGlobalRef(env, value->data.rfunc.reflect_method);
            copy->data.rfunc.obj_inst = (*env)->NewGlobalRef(env, value->data.rfunc.obj_inst);
            break;
        case ENGINE_JAVA_OBJECT:
            copy->data.obj = (*env)->NewGlobalRef(env, value->data.obj);
            break;
        case ENGINE_STRING: {
            size_t len = (strlen(value->data.str) + 1);
            copy->data.str = malloc(sizeof(char) * len);
            memcpy(copy->data.str, value->data.str, len);
            break;
        }
        default:
            memcpy(&(copy->data), &(value->data), sizeof(engine_data));
            break;
    }
    jobject jcopy = engine_wrap(env, value);
    return jcopy;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateObj
 * Signature: ()Ljava/lang/Object;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateObj
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (value->type == ENGINE_JAVA_OBJECT) {
        return value->data.obj;
    }
    else {
        throw(env, "C: tried to translate value to object");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_JAVA_OBJECT) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateString
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateString
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_STRING) {
        return (*env)->NewStringUTF(env, value->data.str);
    }
    else {
        throw(env, "C: tried to translate value to string");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_STRING) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateLong
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateLong
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_FLOATING) {
        return (jlong) value->data.d;
    }
    else if (value->type == ENGINE_INTEGRAL) {
        return (jlong) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to long");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_FLOATING || value->type == ENGINE_INTEGRAL) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateShort
 * Signature: ()S
 */
JNIEXPORT jshort JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateShort
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_FLOATING) {
        return (jshort) value->data.d;
    }
    else if (value->type == ENGINE_INTEGRAL) {
        return (jshort) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to short");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_FLOATING || value->type == ENGINE_INTEGRAL) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateByte
 * Signature: ()B
 */
JNIEXPORT jbyte JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateByte
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_FLOATING) {
        return (jbyte) value->data.d;
    }
    else if (value->type == ENGINE_INTEGRAL) {
        return (jbyte) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to byte");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_FLOATING || value->type == ENGINE_INTEGRAL) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateInt
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateInt
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_FLOATING) {
        return (jint) value->data.d;
    }
    else if (value->type == ENGINE_INTEGRAL) {
        return (jint) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to int");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_FLOATING || value->type == ENGINE_INTEGRAL) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateFloat
 * Signature: ()F
 */
JNIEXPORT jfloat JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateFloat
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_FLOATING) {
        return (jfloat) value->data.d;
    }
    else if (value->type == ENGINE_INTEGRAL) {
        return (jfloat) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to float");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_FLOATING || value->type == ENGINE_INTEGRAL) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateDouble
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateDouble
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_FLOATING) {
        return (jdouble) value->data.d;
    }
    else if (value->type == ENGINE_INTEGRAL) {
        return (jdouble) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to double");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_FLOATING || value->type == ENGINE_INTEGRAL) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    translateBoolean
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_translateBoolean
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    
    if (value->type == ENGINE_BOOLEAN) {
        return (jboolean) value->data.i;
    }
    else {
        throw(env, "C: tried to translate value to byte");
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
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_BOOLEAN) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    canTranslateArray
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_canTranslateArray
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_ARRAY) : 0;
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
    
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type != ENGINE_ARRAY) {
        throw(env, "C: tried to translate value to array");
        return 0;
    }
    // get array component type
    jclass comptype = (*env)->CallObjectMethod(env, array_type, id_comptype);
    // create array from type
    jobject array = (*env)->CallStaticObjectMethod(env, class_array, id_newarray, comptype,
        value->data.array.length);
    uint32_t t;
    for (t = 0; t < value->data.array.length; t++) {
        // get engine_value element, and then get the java counterpart
        jobject wrapped_element = engine_wrap(env, value->data.array.values[t]);
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
    engine_value* value = findnative(env, this);
    return value ? IS_ENGINE_FUNCTION(value->type) : 0;
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    set
 * Signature: (Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)V
 */
JNIEXPORT void JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_set
(JNIEnv* env, jobject this, jobject jkey, jobject jvalue) {
    if (!jkey || !jvalue) return;
    engine_value* this_value = findnative(env, this);
    if (!this_value) return;
    engine_value* key = findnative(env, jkey);
    if (!key) return;
    engine_value* value = findnative(env, jvalue);
    if (!value) return;
    
    if (this_value->type == ENGINE_LUA_GLOBALS) {
        if (!this_value->inst) {
            throw(env, "J->C: globals value is not associated with engine instance");
            return;
        }
        if (key->type != ENGINE_STRING) {
            throw(env, "J->C: tried to set global value with non-string key");
            return;
        }
        else if (key->data.str == 0) {
            throw(env, "J->C: internal error: null string value (bad value)");
            return;
        }
        lua_State* state = this_value->data.state;
        engine_pushvalue(env, this_value->inst, state, value);
        // pops a value from the stack
        lua_setglobal(state, key->data.str);
        
        if (engine_debug) {
            printf("J->C: Set globals with value '%s', value type: %d\n", key->data.str, (int) value->type);
        }
    }
    else {
        throw(env, "J->C: tried to set non-global value");
    }
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    get
 * Signature: (Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_get
(JNIEnv* env, jobject this, jobject script_value) {
    
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    
    engine_value* key = engine_unwrap(env, script_value);
    // this happens if some retard calls this method with a script value that isn't LuaNScriptValue or null
    if (key == 0) {
        throw(env, "J->C: tried to index value with invalid key");
        return 0;
    }
    if (value->type == ENGINE_ARRAY) {
        long t;
        if (key->type != ENGINE_FLOATING && key->type != ENGINE_INTEGRAL) {
            throw(env, "J->C: tried to index value (array) with non-number key");
            return 0;
        }
        else if (key->type == ENGINE_FLOATING) {
            t = (long) key->data.d;
        }
        else if (key->type == ENGINE_INTEGRAL) {
            t = key->data.i;
        }
        
        if (t < value->data.array.length && t >= 0) {
            // get value pointer from array
            engine_value* result = key->data.array.values[t];
            // just in case this happens, handle it
            // actual null values have their own type
            if (result == 0) {
                throw(env, "J->C: internal error: result after indexing array with valid key is somehow a null pointer (bad value table?)");
                return 0;
            }
            // lookup value and get object counterpart
            return engine_wrap(env, result);
        }
        else {
            throw(env, "J->C: tried to index value (array) with out-of-range key");
            return 0;
        }
    }
    // getting this type leaks a value into our table,
    // which means indexing pooploads of globals is a
    // bad idea.
    else if (value->type == ENGINE_LUA_GLOBALS) {
        if (key->type != ENGINE_STRING) {
            throw(env, "J->C: the native backend does not allow indexing globals with non-string values");
            return 0;
        }
        else if (key->data.str == 0) {
            throw(env, "J->C: internal error: null string value (bad value)");
            return 0;
        }
        lua_State* state = value->data.state;
        // push onto stack
        lua_getglobal(state, key->data.str);
        // pops after
        // this function builds a new value (memory!)
        engine_value* retvalue = engine_popvalue(env, value->inst, state);
        if (engine_debug) {
            printf("J->C: Indexed globals with value '%s', resulting type: %d\n", key->data.str, (int) retvalue->type);
        }
        return engine_wrap(env, retvalue);
    }
    else {
        throw(env, "J->C: tried to index non-array/non-global value");
        return 0;
    }
}

static inline jobject handlecall(JNIEnv* env, jobject this, jobjectArray arr) {
    engine_value* value = findnative(env, this);
    if (!value) return 0;
    if (value->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
        throw(env, "J->C: tried to call stub (lambda func)");
        return 0;
    }
    else if (value->type == ENGINE_JAVA_REFLECT_FUNCTION) {
        throw(env, "J->C: tried to call stub (reflect func)");
        return 0;
    }
    else if (value->type == ENGINE_LUA_FUNCTION) {
        if (value->inst) {
            engine_inst* inst = value->inst;
            lua_State* state = inst->state;
            
            lua_getglobal(state, FUNCTION_REGISTRY);
            
            if (lua_isnil(state, -1)) {
                lua_pop(state, 1);
                lua_newtable(state);
                lua_pushvalue(state, -1); // copy
                lua_setglobal(state, FUNCTION_REGISTRY);
            }
            
            lua_pushinteger(state, value->data.func);
            lua_gettable(state, -2);
            
            if (lua_isnil(state, -1)) {
                lua_pop(state, 2);
                throw(env, "J->C: internal error: failed to index function from registry");
                return 0;
            }
            // remove table
            lua_remove(state, -2);
            jsize len = 0;
            if (arr) {
                len = (*env)->GetArrayLength(env, arr);
                int t;
                for (t = 0; t < len; t++) {
                    jobject jvalue = (*env)->GetObjectArrayElement(env, arr, t);
                    if (!jvalue) continue;
                    engine_value* element = engine_unwrap(env, jvalue);
                    if (!element) continue;
                    engine_pushvalue(env, inst, state, element);
                }
            }
            engine_value* ret = engine_call(env, inst, state, len);
            return ret ? engine_wrap(env, ret) : 0;
        }
        else {
            throw(env, "J->C: internal error: lua function is a shared value");
            return 0;
        }   
    }
    else {
        const char* prefix = "J->C: tried to call value as function: ";
        size_t len = strlen(prefix) + 6;
        char message[len];
        memset(message, 0, len);
        strcat(message, prefix);
        sprintf(message + strlen(prefix), "%d", value->type);
        throw(env, message);
        return 0;
    }
}

/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    call
 * Signature: ()Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_call__
(JNIEnv* env, jobject this) {
    return handlecall(env, this, 0);
}
/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    call
 * Signature: ([Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;)Lca/jarcode/consoles/computer/interpreter/interfaces/ScriptValue;
 */
JNIEXPORT jobject JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_call___3Lca_jarcode_consoles_computer_interpreter_interfaces_ScriptValue_2
(JNIEnv* env, jobject this, jobjectArray arr) {
    return handlecall(env, this, arr);
}
/*
 * Class:     ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue
 * Method:    isNull
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_ca_jarcode_consoles_computer_interpreter_luanative_LuaNScriptValue_isNull
(JNIEnv* env, jobject this) {
    engine_value* value = findnative(env, this);
    return value ? (value->type == ENGINE_NULL) : 0;
}
