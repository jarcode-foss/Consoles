#include <jni.h>
#include <jni_md.h>

#include <lua.h>
#include <luajit.h> 
#include <lauxlib.h>
#include <lualib.h>

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <ffi.h>

#include <LuaEngine.h>
#include <setjmp.h>

#include <sys/time.h>

#include "engine.h"
#include "lua_utils.h"

// definitions
jclass class_type = 0;
jmethodID id_comptype = 0;
jclass class_lua = 0;
jmethodID id_translatevalue = 0;
jmethodID id_translate = 0;
jmethodID id_methodresolve = 0;
jmethodID id_methodid = 0;
jclass class_method = 0;
jmethodID id_methodcall = 0;
jmethodID id_methodcount = 0;
jmethodID id_methodtypes = 0;
jclass class_object = 0;
jmethodID id_hashcode = 0;
jclass exclass = 0;
jclass class_ex = 0;
jmethodID id_getmessage = 0;
jmethodID id_classname = 0;
jmethodID id_exhandle = 0;

uint32_t function_index = 0;

int engine_debug = 0;
static uint8_t setup = 0;

// we use a single caller interface for wrapping Java -> C -> Lua functions,
// since they all boil down to 'void (*lua_cfunc) (lua_State* state)'
static ffi_cif func_cif;
// again, this is the single caller interface used for hook functions
static ffi_cif hook_cif;

volatile static int maxtime = 7000;

static ffi_type* f_args[1];
static ffi_type* h_args[2];

static void freewrapper(engine_inst* inst, JNIEnv* env, engine_jfuncwrapper* wrapper);
static void engine_pushreflect_skip(JNIEnv* env, engine_inst* inst, jobject reflect_method,
                                    jobject obj_inst, uint8_t skip_first);

/*
 * There are some resources that accumulate over the life of the Lua VM. The reason
 * why we cannot collect these during the VM's lifetime is because it's impossible
 * to determine when they are no longer needed, and because it's not plausible
 * to rely on Lua code (as it is untrusted) to close these resources.
 * 
 * So, we have to close them at the end of the lifecycle.
 */
void engine_close(JNIEnv* env, engine_inst* inst) {
    
    // close lua entirely
    lua_close(inst->state);
    
    // if the engine was already marked closed, do nothing
    if (inst->closed) return;
    
    // Free all function wrappers (and underlying ffi closures).
    // the amount of closures registered will continue to grow as
    // java functions are registered, and will only be free'd when
    // the entire engine is closed.
    int t;
    for (t = 0; t < inst->wrappers_amt; t++) {
        freewrapper(inst, env, inst->wrappers[t]);
    }
    
    // free wrapper stack
    if (inst->wrappers) {
        free(inst->wrappers);
        inst->wrappers = 0;
    }
    inst->wrappers_amt = 0;
    
    // free closure used for hook function
    ffi_closure_free(inst->closure);
    
    // free instance struct
    free(inst);
}

static void freewrapper(engine_inst* inst, JNIEnv* env, engine_jfuncwrapper* wrapper) {
    
    // free closure
    ffi_closure_free(wrapper->closure);
    
    // delete global reference to java function
    if (wrapper->obj_inst) { // if it's null, it was for a reflected static function
        (*env)->DeleteGlobalRef(env, wrapper->obj_inst);
    }
    
    // reflected methods have a Method instance to be deleted
    if (wrapper->type == ENGINE_JAVA_REFLECT_FUNCTION) {
        (*env)->DeleteGlobalRef(env, wrapper->data.reflect.method);
    }
    else if (wrapper->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
        (*env)->DeleteGlobalRef(env, wrapper->data.lambda.class_array);
    }
    
    free(wrapper);
}

// unused, probably should be used
void engine_removewrapper(engine_inst* inst, engine_jfuncwrapper* wrapper) {
    if (inst->wrappers_amt == 0) return;
    if (inst->wrappers_amt == 1) {
        free(inst->wrappers);
        inst->wrappers_amt = 0;
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
        if (t != inst->wrappers_amt - 1) {
            engine_jfuncwrapper** ptr = &(inst->wrappers[t]); // pointer to element t
            memmove(ptr, ptr + 1, (inst->wrappers_amt - (t + 1)) * sizeof(void*));
        }
        inst->wrappers = realloc(inst->wrappers, (inst->wrappers_amt - 1) * sizeof(void*));
        inst->wrappers_amt--;
    }
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

static void hook(engine_inst* inst, lua_State* state, lua_Debug* debug) {
    
    // when we've figured out that the script has been killed, we
    // need to constantly error out of each function until we're
    // out of the script. Gernerally we only need to do this once,
    // but lua programs have the possibility to trap their own errors,
    // so this ensures that no lua code can continue to run
    //
    // there is an SO answer on this problem, and somehow the
    // accepted solution is to use setjmp/longjmp. That is a terrible
    // solution, because we're dealing with our own ffi functions
    // (unsafe to jmp out of), and luajit (jit compiled, lots of
    // undefined stuff could happen), and other internal ffi usages.
    
    if (!(inst->killed) && maxtime > 0) {
        struct timeval last;
        gettimeofday(&last, 0);
        unsigned long ms = (unsigned long) (last.tv_usec / 1000U) + (last.tv_sec * 1000U);

        if (!(inst->last_interrupt)) {
            inst->last_interrupt = ms;
        }
        else if (ms - inst->last_interrupt > maxtime) {
            inst->killed = 1;
        }
    }
    if (inst->killed) {
        // re-set hook to constantly execute
        lua_sethook(state, inst->hook, LUA_MASKLINE, 0);
        // error
        luaL_error(state, "C: killed");
    }
}

// binding hook for ffi
static void handle_hook(ffi_cif* cif, void* ret, void* args[], void* user_data) {
    hook((engine_inst*) user_data, *(lua_State**) args[0], *(lua_Debug**) args[1]);
}

static inline void abort_ffi() {
    fprintf(stderr, "\nfailed to prepare ffi caller interface for C function wrappers\n");
    engine_abort();
}

static inline void abort_ffi_alloc() {
    fprintf(stderr, "\nfailed to allocate ffi closure\n");
    engine_abort();
}

static inline void abort_ffi_prep() {
    fprintf(stderr, "\nfailed to prepare ffi closure\n");
    engine_abort();
}

static void setup_closures() {
    if (!FFI_CLOSURES) {
        fprintf(stderr, "\nFFI_CLOSURES are not supported on this architecture (libffi)\n");
        engine_abort();
    }
    
    // ffi function args
    f_args[0] = &ffi_type_pointer;
    // prepare caller interface
    if (ffi_prep_cif(&func_cif, FFI_DEFAULT_ABI, 1, &ffi_type_sint, f_args) != FFI_OK) {
        abort_ffi();
    }

    // args for hook function
    h_args[0] = &ffi_type_pointer;
    h_args[1] = &ffi_type_pointer;
    if (ffi_prep_cif(&hook_cif, FFI_DEFAULT_ABI, 2, &ffi_type_void, h_args) != FFI_OK) {
        abort_ffi();
    }
}

static void setup_classes(JNIEnv* env, jmp_buf handle) {
    
    // class registering
    classreg(env, "java/lang/Object", &class_object, handle);
    classreg(env, ENGINE_CLASS, &class_type, handle);
    classreg(env, ENGINE_LUA_CLASS, &class_lua, handle);
    classreg(env, ENGINE_ERR_CLASS, &exclass, handle);
    classreg(env, "java/lang/reflect/Method", &class_method, handle);
    classreg(env, "java/lang/Throwable", &class_ex, handle);
    
    // Object ids
    id_hashcode = method_resolve(env, class_object, "hashCode", "()I", handle);
    
    // Method ids
    id_methodcall = method_resolve(env, class_method, "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", handle);
    id_methodcount = method_resolve(env, class_method, "getParameterCount", "()I", handle);
    id_methodtypes = method_resolve(env, class_method, "getParameterTypes", "()[Ljava/lang/Class;", handle);
    
    // Class ids
    id_comptype = method_resolve(env, class_type, "getComponentType", "()Ljava/lang/Class;", handle);
    id_classname = method_resolve(env, class_type, "getSimpleName", "()Ljava/lang/String;", handle);
    
    // Lua ids
    id_methodresolve = static_method_resolve(env, class_lua, "resolveMethod", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/reflect/Method;", handle);
    id_methodid = static_method_resolve(env, class_lua, "methodId", "(Ljava/lang/reflect/Method;)J", handle);
    id_exhandle = static_method_resolve(env, class_lua, "handleJavaException", "(Ljava/lang/Throwable;)V", handle);

    // Throwable ids
    id_getmessage = method_resolve(env, class_ex, "getMessage", "()Ljava/lang/String;", handle);
    
    char buf[128] = {0};
    strcat(buf, "(Ljava/lang/Object;)L");
    strcat(buf, ENGINE_VALUE_INTERFACE);
    strcat(buf, ";");
    id_translatevalue = static_method_resolve(env, class_lua, "translateToScriptValue", buf, handle);
    
    memset(buf, 0, sizeof(buf));
    strcat(buf, "(Ljava/lang/Class;L");
    strcat(buf, ENGINE_VALUE_INTERFACE);
    strcat(buf, ";)Ljava/lang/Object;");
    id_translate = static_method_resolve(env, class_lua, "translate", buf, handle);

    ASSERTEX(env);
}

// This function allows users to release the global reference
// from an object early. This isn't required, but it's nice.
static int engine_releaseobj(lua_State* state) {
    // first arg should be userdata
    engine_userdata* d = (engine_userdata*) luaL_checkudata(state, 1, ENGINE_USERDATA_TYPE);
    if (d && !(d->released)) {
        JNIEnv* env  = d->engine->runtime_env;
        (*env)->DeleteGlobalRef(env, d->obj);
        d->released = 1;
    }
    return 0;
}

// this is a function that handles indexing on userdata
// this can be optimized, but to improve speed, all types passed to this layer
// would have to be mapped out (and cleaned up), instead of doing it on the fly.
int engine_handleobjcall(lua_State* state) {
    // first arg should be userdata
    engine_userdata* d = luaL_checkudata(state, 1, ENGINE_USERDATA_TYPE);
    luaL_argcheck(state, d != 0, 1, "`interface' expected");
    // second arg should be string, indexing a java object with anything else makes no sense
    const char* str = lua_tostring(state, 2);
    
    jobject obj = d->obj;
    JNIEnv* env = d->engine->runtime_env;
    if (!env) {
        luaL_error(state, "C: internal error: bad JNI environment");
        return 0;
    }
    else if (!obj) {
        luaL_error(state, "C: internal error: null object reference");
        return 0;
    }
    else if (d->released) {
        luaL_error(state, "C: object released");
        return 0;
    }
    if (!strcmp(str, "release")) {
        lua_pushcfunction(state, &engine_releaseobj);
        return 1;
    }
    int ret = 1;
    jstring jstr = (*env)->NewStringUTF(env, str);
    
    ASSERTEX(env);
    
    jobject method = (*env)->CallStaticObjectMethod(env, class_lua, id_methodresolve, obj, jstr);
    (*env)->DeleteLocalRef(env, jstr);
    
    ASSERTEX(env);
    
    if (method) {
        engine_pushreflect_skip(env, d->engine, method, obj, 1);
        (*env)->DeleteLocalRef(env, method);
    }
    else {
        luaL_error(state, "C: could not resolve method");
        ret = 0;
    }
    return ret;
}

JNIEXPORT void JNICALL Java_jni_LuaEngine_setup(JNIEnv* env, jobject object) {
    if (!setup) {
        static jmp_buf reg_handle;
        
        if (setjmp(reg_handle)) { return; }
        
        function_index = 0;
        
        setup_closures();
        setup_classes(env, reg_handle);
        setup_value(env, reg_handle);
        thread_datum_init(env, reg_handle);

        ASSERTEX(env);
        
        setup = 1;
    }
}

JNIEXPORT jlong JNICALL Java_jni_LuaEngine_setupinst
(JNIEnv* env, jobject this, jint mode, jlong heap, jint interval) {
    
    engine_inst* instance = malloc(sizeof(engine_inst));
    memset(instance, 0, sizeof(engine_inst));
    
    instance->restricted = 1;
    instance->interval = interval;
    
    void* hook_binding = 0;
    instance->closure = ffi_closure_alloc(sizeof(ffi_closure), &hook_binding); // allocate hook closure
    if (!(instance->closure)) abort_ffi_alloc();
    
    if (ffi_prep_closure_loc(instance->closure, &hook_cif, &handle_hook, instance, hook_binding) != FFI_OK) {
        abort_ffi_prep();
    }
    
    lua_State* state = lua_open();
    
    // set LuaJIT mode
    switch (mode) {
        case 1:
            luaJIT_setmode(state, 0, LUAJIT_MODE_ENGINE | LUAJIT_MODE_ON);
            break;
        case 0:
            luaJIT_setmode(state, 0, LUAJIT_MODE_ENGINE | LUAJIT_MODE_OFF);
            break;
    }
    
    luaopen_base(state);
    luaopen_table(state);
    luaopen_math(state);
    luaopen_string(state);
    luaopen_table(state);
    luaopen_debug(state);
    
    // I/O is handled by overloading print/write in Java code. Unlike LuaJ (which does its own
    // internal magic and needs a stream to print to), LuaJIT just calls print() and write(),
    // which are already likely c functions.
    
    // assign hook function ptr for later use in kill handling
    instance->hook = (lua_Hook) hook_binding;
    
    // set hook to run over an interval
    lua_sethook(state, (lua_Hook) hook_binding, LUA_MASKCOUNT, interval);
    
    // register generic userdata table (for java objects)
    luaL_newmetatable(state, ENGINE_USERDATA_TYPE);
    // we are setting the __index key, which lua calls every time it tries to index an object
    lua_pushstring(state, "__index");
    // attach our function to handle generic calls into an object
    lua_pushcfunction(state, &engine_handleobjcall);
    // set key and value
    lua_rawset(state, -3);
    // push unique __target value, we use this to identify our userdata from others.
    lua_pushstring(state, "__target");
    // push some value
    lua_pushboolean(state, 1);
    // set key and value
    lua_rawset(state, -3);
    // __gc method for java object references
    lua_pushstring(state, "__gc");
    // release function, this is also exposed to lua
    lua_pushcfunction(state, &engine_releaseobj);
    // set
    lua_rawset(state, -3);
    // pop metatable off the stack
    lua_pop(state, 1);
    
    // set the implementation type
    lua_pushstring(state, ENGINE_TYPE);
    lua_setglobal(state, ENGINE_TYPE_KEY);
    
    instance->state = state;

    util_setup(instance, state);
    
    return (jlong) (uintptr_t) instance;
}

// this can be called from any thread!
// we shouldn't have to worry about anything here though, just a few dereferencing and
// setting a volatile flag.
JNIEXPORT void JNICALL Java_jni_LuaEngine_kill(JNIEnv* env, jobject this, jlong ptr) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    inst->killed = 1;
}

struct engine_program {
    const char* str;
    uint8_t read;
};

static const char* loadchunk(lua_State* state, void* data, size_t* size) {
    struct engine_program* program = (struct engine_program*) data;
    if (program->read) {
        *size = 0;
        return 0;
    }
    else program->read = 1;
    *size = strlen(program->str);
    return program->str;
}

JNIEXPORT jobject JNICALL Java_jni_LuaEngine_load(JNIEnv* env, jobject this, jlong ptr, jstring jraw, jstring jpath) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    lua_State* state = inst->state;
    
    const char* raw = (*env)->GetStringUTFChars(env, jraw, 0);
    const char* path = (*env)->GetStringUTFChars(env, jpath, 0);

    struct engine_program program = {raw, 0};
    
    int result = lua_load(state, (lua_Reader) loadchunk, &program, path);
    
    (*env)->ReleaseStringUTFChars(env, jraw, raw);
    (*env)->ReleaseStringUTFChars(env, jpath, path);
    
    if (result) {
        if (lua_isstring(state, -1)) {
            const char* message = lua_tostring(state, -1);
            throw(env, message);
            lua_pop(state, 1);
        }
        else {
            throw(env, "C: encountered a non-standard error while trying to load chunk");
        }
        return 0;
    }
    
    engine_value* value = engine_newvalue(env, inst);
    engine_handleregistry(env, inst, state, value);
    return engine_wrap(env, value);
}

JNIEXPORT jlong JNICALL Java_jni_LuaEngine_unrestrict(JNIEnv* env, jobject this, jlong ptr) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    if (inst->restricted) {
        luaL_openlibs(inst->state);
        lua_sethook(inst->state, inst->hook, LUA_MASKCOUNT, inst->interval);
        inst->restricted = 0;
    }
    return ptr;
}

JNIEXPORT void JNICALL Java_jni_LuaEngine_settable
(JNIEnv* env, jobject this, jlong ptr, jstring jtable, jstring jkey, jobject jvalue) {
    
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    
    JNIEnv* temp_env = inst->runtime_env;
    inst->runtime_env = env;
    
    lua_State* state = inst->state;
    const char* table = (*env)->GetStringUTFChars(env, jtable, 0);
    const char* key = (*env)->GetStringUTFChars(env, jkey, 0);
    
    // get and push table
    lua_getglobal(state, table);
    // is nil
    if (lua_isnil(state, -1)) {
        // pop nil
        lua_pop(state, 1);
        // push new table
        lua_newtable(state);
        // make copy
        lua_pushvalue(state, -1);
        // set global table and pop copy
        lua_setglobal(state, table);
    }
    // push key
    lua_pushstring(state, key);
    // push value
    engine_value* value = engine_unwrap(env, jvalue);
    if (value) {
        engine_pushvalue(env, inst, state, value);
    }
    else {
        lua_pushnil(state);
    }
    // set table, pops key & value
    lua_settable(state, -3);
    // pop table
    lua_pop(state, 1);
    
    (*env)->ReleaseStringUTFChars(env, jtable, table);
    (*env)->ReleaseStringUTFChars(env, jkey, key);

    inst->runtime_env = temp_env;
}

JNIEXPORT jint JNICALL Java_jni_LuaEngine_destroyinst(JNIEnv* env, jobject this, jlong ptr) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    inst->runtime_env = env;
    engine_close(env, inst);
    return 0;
}

JNIEXPORT void JNICALL
Java_jni_LuaEngine_setdebug(JNIEnv* env, jobject this, jint mode) {
    engine_debug = mode;
    setvbuf(stdout, NULL, _IONBF, 0); /* Output buffering is annoying when it screws up message order */
}

JNIEXPORT void JNICALL
Java_jni_LuaEngine_interruptreset(JNIEnv* env, jobject this, jlong ptr) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    
    struct timeval last;
    gettimeofday(&last, 0);
    inst->last_interrupt = (unsigned long) (last.tv_usec / 1000U) + (last.tv_sec * 1000U);
}

JNIEXPORT void JNICALL
Java_jni_LuaEngine_setmaxtime(JNIEnv* env, jobject this, jint suggested_maxtime) {
    maxtime = suggested_maxtime;
}

JNIEXPORT void JNICALL
Java_jni_LuaEngine_blacklist(JNIEnv* env, jobject this, jlong ptr) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    util_blacklist(inst->state);
}

JNIEXPORT void JNICALL
Java_jni_LuaEngine_thread_1end(JNIEnv* env, jobject this) {
    // This doesn't do anything anymore, keeping it for other cleanup operations.
}

JNIEXPORT jobject JNICALL Java_jni_LuaEngine_wrapglobals(JNIEnv* env, jobject this, jlong ptr) {
    engine_inst* inst = (engine_inst*) (uintptr_t) ptr;
    engine_value* v = engine_newvalue(env, inst);
    v->type = ENGINE_LUA_GLOBALS;
    v->data.state = inst->state;
    return engine_wrap(env, v);
}

// Converts a java exception into a Lua error, and pops a reference frame.
static void expass_pop(JNIEnv* env, lua_State* state, engine_jfuncwrapper* wrapper) {

    // if an exception occurred, we need to handle it and pass it to lua
    // we also call a Java helper method to further handle the exception.
    if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
        
        jobject ex = (*env)->ExceptionOccurred(env);
        (*env)->ExceptionClear(env);

        ASSERTEX(env);
        
        jobject jmsg = (*env)->CallObjectMethod(env, ex, id_getmessage);
        jclass type_ex = (*env)->GetObjectClass(env, ex);
        jobject jtype = (*env)->CallObjectMethod(env, type_ex, id_classname);

        ASSERTEX(env);
        
        const char* characters = jmsg ? (*env)->GetStringUTFChars(env, jmsg, 0) : 0;
        const char* type_chars = (*env)->GetStringUTFChars(env, jtype, 0);
        
        char stack_characters[(jmsg ? strlen(characters) : 0) + 1];
        if (jmsg) {
            stack_characters[strlen(characters)] = '\0';
            memmove(stack_characters, characters, strlen(characters));
        }
        char stack_type_chars[strlen(type_chars) + 1];
        stack_type_chars[strlen(type_chars)] = '\0';
        memmove(stack_type_chars, type_chars, strlen(type_chars));
        
        (*env)->ReleaseStringUTFChars(env, jtype, characters);
        if (jmsg) {
            (*env)->ReleaseStringUTFChars(env, jmsg, type_chars);
        }

        (*env)->CallStaticVoidMethod(env, class_lua, id_exhandle, ex);
        
        (*env)->PopLocalFrame(env, NULL);
        
        if (jmsg) {
            luaL_error(state, "J: %s -> %s", stack_type_chars, stack_characters);
        }
        else {
            luaL_error(state, "J: %s", stack_type_chars);
        }
    }
}

// this is a (wrapped) function that handles _all_ Lua -> Java function calls
// it's also wrapped into its own stack frame, so all local references will be
// cleaned up after returning
static int engine_handlecall_frame(engine_jfuncwrapper* wrapper, lua_State* state) {

    ASSERTEX(wrapper->engine->runtime_env);
    
    // This is a fix for the ':' operator for function
    // calls on interfaces. We discard the first
    // argument if nessecary.
    
    if (wrapper->skip_first) {
        lua_remove(state, 1);
    }
    
    JNIEnv* env = wrapper->engine->runtime_env;
    engine_inst* inst = wrapper->engine;
    
    int vargs = 0;
    switch (wrapper->type) {
    case ENGINE_JAVA_LAMBDA_FUNCTION:
        vargs = (*env)->GetArrayLength(env, wrapper->data.lambda.class_array);
        break;
    case ENGINE_JAVA_REFLECT_FUNCTION:
        vargs = (*env)->CallIntMethod(env, wrapper->data.reflect.method, id_methodcount);
        break;
    }
    
    ASSERTEX(wrapper->engine->runtime_env);
    
    // Lua doesn't know that it needs to match the function/method signature, so we can expect bad arguments
    // this is technically valid Lua code and errors shouldn't be thrown, but we'll still complain if debug
    // mode is enabled.
    int passed = lua_gettop(state);
    if (passed > vargs && engine_debug) {
        printf("C: calling java function with bad arguments (expected: %d, got: %d)\n", vargs, passed);
    }
    // however, if we don't have enough arguments to call the java function, then
    // we'll need to error.
    else if (passed < vargs) {
        luaL_error(state, "C: not enough arguments (expected: %d, got: %d)", vargs, passed);
        return 0;
    }
    
    // if the stack is corrupted, this is a good indicator
#if ENGINE_CDEBUG > 0
    if (passed < 0 || passed > 5000) {
        printf("FATAL: corrupt Lua API stack\n");
        exit(EXIT_FAILURE);
    }
#endif
    
    // something should be done to truncate extra arguments, because we're
    // operating on the top of the stack
    lua_settop(state, (int) vargs); // truncate
    
    engine_value* v_args[vargs];
    
    // backwards iterate so we get our arguments in order
    int t;
    for (t = vargs - 1; t >= 0; t--) {
        v_args[t] = engine_popvalue(env, inst, state);
        // if null pointer, create new nill value
        if (!v_args[t]) {
            v_args[t] = engine_newvalue(env, inst);
        }
    }
    
    // return value (in java)
    jobject ret = 0;
    // argument array for reflection
    jobjectArray arr = 0;

    ASSERTEX(env);
    
    // You cannot magically pass varadic amounts between functions in C,
    // so this is a bit ugly.
    //
    // On the bright side, this is actually really fast. The method ids
    // were dynamically resolved on creation of the closure, so every
    // time a 'lambda' function is called, it only ends up being a
    // single JNI call.
    //
    // I also could do this with Lua.callAndRelease(...), but I avoid
    // so much more overhead doing it this way.
    if (wrapper->type == ENGINE_JAVA_LAMBDA_FUNCTION) {
        static jmp_buf buf;
        
        if (setjmp(buf)) {
            goto cleanup;
        }
        
        jobject paramtypes = wrapper->data.lambda.class_array;
        if (wrapper->data.lambda.ret) {
            switch (vargs) {
            case 0:
                ret = (*env)->CallObjectMethod(env, wrapper->obj_inst, wrapper->data.lambda.id);
                break;
            case 1:
                ret = (*env)->CallObjectMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                               TOJAVA(env, v_args, paramtypes, 0, buf));
                break;
            case 2:
                ret = (*env)->CallObjectMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                               TOJAVA(env, v_args, paramtypes, 0, buf),
                                               TOJAVA(env, v_args, paramtypes, 1, buf));
                break;
            case 3:
                ret = (*env)->CallObjectMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                               TOJAVA(env, v_args, paramtypes, 0, buf),
                                               TOJAVA(env, v_args, paramtypes, 1, buf),
                                               TOJAVA(env, v_args, paramtypes, 2, buf));
                break;
            case 4:
                ret = (*env)->CallObjectMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                               TOJAVA(env, v_args, paramtypes, 0, buf),
                                               TOJAVA(env, v_args, paramtypes, 1, buf),
                                               TOJAVA(env, v_args, paramtypes, 2, buf),
                                               TOJAVA(env, v_args, paramtypes, 3, buf));
                break;
            }
        }
        else {
            switch (vargs) {
            case 0:
                (*env)->CallVoidMethod(env, wrapper->obj_inst, wrapper->data.lambda.id);
                break;
            case 1:
                (*env)->CallVoidMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                       TOJAVA(env, v_args, paramtypes, 0, buf));
                break;
            case 2:
                (*env)->CallVoidMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                       TOJAVA(env, v_args, paramtypes, 0, buf),
                                       TOJAVA(env, v_args, paramtypes, 1, buf));
                break;
            case 3:
                (*env)->CallVoidMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                       TOJAVA(env, v_args, paramtypes, 0, buf),
                                       TOJAVA(env, v_args, paramtypes, 1, buf),
                                       TOJAVA(env, v_args, paramtypes, 2, buf));
                break;
            case 4:
                (*env)->CallVoidMethod(env, wrapper->obj_inst, wrapper->data.lambda.id,
                                       TOJAVA(env, v_args, paramtypes, 0, buf),
                                       TOJAVA(env, v_args, paramtypes, 1, buf),
                                       TOJAVA(env, v_args, paramtypes, 2, buf),
                                       TOJAVA(env, v_args, paramtypes, 3, buf));
                break;
            }
        }
    }
    
    // this has a lot of overhead, but we really don't have any other choice for reflected
    // functions.
    else if (wrapper->type == ENGINE_JAVA_REFLECT_FUNCTION) {
        
        // Class[]
        jobject paramtypes = (*env)->CallObjectMethod(env, wrapper->data.reflect.method, id_methodtypes);
        
        ASSERTEX(env);
        
        // Object[]
        arr = (*env)->NewObjectArray(env, vargs, class_object, 0);

        ASSERTEX(env);
        
        for (t = 0; t < vargs; t++) {
            // get element type (Class)
            jobject element_type = (*env)->GetObjectArrayElement(env, paramtypes, t);
            // get corresponding ScriptValue
            jobject element = engine_wrap(env, v_args[t]);

            ASSERTEX(env); /* it's a fatal error if we can't obtain the target argument types */
            
            // translate to Object
            jobject translated = (*env)->CallStaticObjectMethod
                (env, class_lua, id_translate, element_type, element);
    
            // pass exception to Lua, if any occurred during translation
            if ((*env)->ExceptionCheck(env) == JNI_TRUE) {
                goto cleanup;
            }
            
            // set index at Object[] array to element
            (*env)->SetObjectArrayElement(env, arr, t, translated);
            
            ASSERTEX(env);

            (*env)->DeleteLocalRef(env, element_type);
            (*env)->DeleteLocalRef(env, translated);
        }
            
        ASSERTEX(env);
        
        // call Method
        ret = (*env)->CallObjectMethod(env, wrapper->data.reflect.method, id_methodcall, wrapper->obj_inst, arr);
    }
    
    // all the argument (engine) values were created just now,
    // and won't be used for anything else.
    //
    // We could further improve this by allocating the engine
    // values on the stack (and reworking some other functions),
    // but that is for another day.
    cleanup: for (t = 0; t < vargs; t++) {
        engine_releasevalue(env, v_args[t]);
    }
    
    // pass exception to Lua, if any occurred (this can jmp out)
    // this also pops a reference frame, if an exception occurred
    expass_pop(env, state, wrapper);
            
    // directly returned null, or no return value, just push nil
    if (!ret) {
        lua_pushnil(state);
        return 1;
    }
    
    // call back into java to map the java value to our factory, and then spit out
    // the ScriptValue [ Lua.translateToScriptValue(Object) ]
    jobject wrapped = (*env)->CallStaticObjectMethod(env, class_lua, id_translatevalue, ret);
        
    // release local reference to the engine_value* from the java side.
    (*env)->DeleteLocalRef(env, ret);
    
    // unwrap and push
    
    // translate returned null, push nil
    if (!wrapped) {
        lua_pushnil(state);
    }
    else {
        engine_value* v = engine_unwrap(env, wrapped);
        
        // if there is no mapped value, something went wrong (premature release?), just push nil
        if (!v) {
            lua_pushnil(state);
        }
        else {
            engine_pushvalue(env, inst, state, v);
            // release value, we won't be seeing this again
            engine_releasevalue(env, v);
        }
    }
    
    return 1;
}

int engine_handlecall(engine_jfuncwrapper* wrapper, lua_State* state) {
    // another binding, this is for pushing a JNI frame.
    JNIEnv* env = wrapper->engine->runtime_env;
    (*env)->PushLocalFrame(env, 128);
    int ret = engine_handlecall_frame(wrapper, state);
    (*env)->PopLocalFrame(env, NULL);
    return ret;
}

// binding function for ffi
void engine_handlecall_binding(ffi_cif* cif, void* ret, void* args[], void* user_data) {
    *(ffi_arg*) ret = engine_handlecall((engine_jfuncwrapper*) user_data, *(lua_State**) args[0]);
}

void engine_getlambdainfo(JNIEnv* env, engine_inst* inst, jclass jfunctype,
                          jobject class_array, engine_lambda_info* info) {
    
    jfieldID fid_return = (*env)->GetStaticFieldID(env, jfunctype, "C_RETURN", "I");
    jint ret = (*env)->GetStaticIntField(env, jfunctype, fid_return);
    jint args = (*env)->GetArrayLength( env, class_array);
    *info = (engine_lambda_info) {.ret = ret, .args = args};
}

// magic to turn Java lambda function wrapper (NoArgFunc, TwoArgVoidFunc, etc) into a C function
// and then pushes it onto the lua stack.
void engine_pushlambda(JNIEnv* env, engine_inst* inst, jobject jfunc, jobject class_array) {
    
    ASSERTEX(env);
    
    // get class
    jclass jfunctype = (*env)->GetObjectClass(env, jfunc);
    
    // obtain func (lambda) info
    uint8_t ret, args;
    {
        engine_lambda_info info;
        engine_getlambdainfo(env, inst, jfunctype, class_array, &info);
        
        ASSERTEX(env);
        
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
    
    static jmp_buf handle;
    
    if (setjmp(handle)) {
        fprintf(stderr, "C: SEVERE: failed to resolve call(?) method for lambda (%s)\n", buf);
        return;
    }
    
    jmethodID mid = method_resolve(env, jfunctype, "call", buf, handle);
    void* func_binding = 0; // our function pointer
    ffi_closure* closure = ffi_closure_alloc(sizeof(ffi_closure), &func_binding); // ffi closure
    
    // this shouldn't happen
    if (!closure) {
        abort_ffi_alloc();
    }
    
    engine_jfuncwrapper* wrapper = malloc(sizeof(engine_jfuncwrapper));
    engine_regwrapper(inst, wrapper);
    
    if (ffi_prep_closure_loc(closure, &func_cif, &engine_handlecall_binding, wrapper, func_binding) != FFI_OK) {
        abort_ffi_prep();
    }

    if (engine_debug) {
        printf("C: wrapping java lambda function (signature: '%s')\n", buf);
        if (class_array) {
            printf("C: method parameter types: %d\n", (*env)->GetArrayLength(env, class_array));
        }
        else {
            printf("C: SEVERE: null parameter types");
        }
    }
    
    wrapper->closure = closure;
    wrapper->type = ENGINE_JAVA_LAMBDA_FUNCTION;
    wrapper->data.lambda.ret = (uint8_t) ret;
    wrapper->data.lambda.class_array = (*env)->NewGlobalRef(env, class_array);
    wrapper->data.lambda.id = mid;
    wrapper->obj_inst = (*env)->NewGlobalRef(env, jfunc);
    wrapper->engine = inst;
    wrapper->skip_first = 0;
    
    lua_pushcfunction(inst->state, (lua_CFunction) func_binding);

    (*env)->DeleteLocalRef(env, jfunctype);
}

// same idea as above, but with reflection types instead (Method). We also do a lookup in
// this implementation to find methods that have already been wrapped to consverve memory over
// the lifetime of a lua VM/interpreter.
static void engine_pushreflect_skip(JNIEnv* env, engine_inst* inst, jobject reflect_method,
                                    jobject obj_inst, uint8_t skip_first) {
    
    ASSERTEX(env);
    
    // compute method id
    long id = (*env)->CallStaticLongMethod(env, class_lua, id_methodid, reflect_method);
    
    ASSERTEX(env);
    // search for reflect wrapper with equal id
    size_t t;
    for(t = 0; t < inst->wrappers_amt; t++) {
        engine_jfuncwrapper* wrapper = inst->wrappers[t];
        if (wrapper->type == ENGINE_JAVA_REFLECT_FUNCTION && wrapper->data.reflect.reflect_id == id) {
            // found identical wrapper, recycle it and return;
            lua_pushcfunction(inst->state, wrapper->func);
            return;
        }
    }
    
    void* func_binding = 0; // our function pointer
    ffi_closure* closure = ffi_closure_alloc(sizeof(ffi_closure), &func_binding); // ffi closure
    
    // this shouldn't happen
    if (!closure) {
        abort_ffi_alloc();
    }
    
    engine_jfuncwrapper* wrapper = malloc(sizeof(engine_jfuncwrapper));
    engine_regwrapper(inst, wrapper);
    
    if (ffi_prep_closure_loc(closure, &func_cif, &engine_handlecall_binding, wrapper, func_binding) != FFI_OK) {
        abort_ffi_prep();
    }
    
    wrapper->closure = closure;
    wrapper->type = ENGINE_JAVA_REFLECT_FUNCTION;
    wrapper->data.reflect.method = (*env)->NewGlobalRef(env, reflect_method);
    wrapper->data.reflect.reflect_id = id;
    wrapper->obj_inst = (*env)->NewGlobalRef(env, obj_inst);
    wrapper->engine = inst;
    wrapper->func = (lua_CFunction) func_binding;
    wrapper->skip_first = skip_first;
    
    lua_pushcfunction(inst->state, (lua_CFunction) func_binding);
    
    if (engine_debug) {
        printf("C: wrapped java reflect function (id: %ld, ptr: %p)\n", id, func_binding);
    }
}

void engine_pushreflect(JNIEnv* env, engine_inst* inst, jobject reflect_method, jobject obj_inst) {
    engine_pushreflect_skip(env, inst, reflect_method, obj_inst, 0);
}

// this is how we handle function calls from Java
engine_value* engine_call(JNIEnv* env, engine_inst* inst, lua_State* state, int nargs) {
            
    ASSERTEX(env);
            
    inst->runtime_env = env;

    if (!lua_isfunction(state, -nargs - 1) && !lua_iscfunction(state, -nargs - 1)) {
        throw(env, "C: internal error: engine_call(...) called without function on stack");
        lua_pop(state, nargs + 1);
        return 0;
    }
    
    // A lot of things can happen in lua code, every time we jump from Java -> Lua,
    // we need another reference frame for local references to be stored.
    (*env)->PushLocalFrame(env, 128);
    
    int err = 0;
    if (!(inst->killed))
        err = lua_pcall(state, nargs, 1, 0);
    else {
        // clear values off stack, don't want to corrupt it even if the instance was killed
        lua_pop(state, nargs + 1);
    }

    // pop the reference frame
    (*env)->PopLocalFrame(env, NULL);

    ASSERTEX(env);
    
    uint8_t abort = 1;
    switch (err) {
    case LUA_ERRRUN: { // runtime error
        const char* str = lua_tostring(state, -1);
        const char* prefix = "C: runtime error: ";
        size_t len = strlen(prefix) + strlen(str) + 1;
        char message[len];
        memset(message, 0, len);
        strcat(message, prefix);
        strcat(message, str);
        throw(env, message);
        lua_pop(state, 1);
        break;
    }
    case LUA_ERRMEM: // memory alloc error (no lua error is thrown)
        throw(env, "C: memory allocation error");
        lua_pop(state, 1);
        break;
    case LUA_ERRERR: // error in error handler
        throw(env, "C: error in error handler");
        lua_pop(state, 1);
        break;
    case 0:
        abort = 0;
        break;
    }
    engine_value* ret = 0;
    if (!abort) {
        ret = engine_popvalue(env, inst, state);
    }
    return ret;
}
