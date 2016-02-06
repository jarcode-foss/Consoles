
#include <jni.h>
#include <jni_md.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <setjmp.h>
#include <pthread.h>
#include <unistd.h>

#include <lua.h>
#include <luajit.h> 
#include <lauxlib.h>
#include <lualib.h>

#include "engine.h"

static void blacklist(lua_State* state, const char* key) {
    lua_pushnil(state);
    lua_setglobal(state, key);
}

static void blacklist_table(lua_State* state, const char* table, const char* key) {
    lua_getglobal(state, table);
    if (lua_istable(state, -1)) {
        lua_pushstring(state, key);
        lua_pushnil(state);
        lua_rawset(state, -3);
    }
    lua_pop(state, 1);
}

int util_blacklist(lua_State* state) {

    if (engine_debug) {
        printf("C: blacklisting globals and table values\n");
    }
    
    blacklist(state, "debug"); // way too many loopholes
    blacklist(state, "coroutine"); // safe, but not needed
    blacklist(state, "io"); // access to native filesystem, remove!
    blacklist(state, "package"); // package system is replaced by Consoles
    blacklist(state, "require"); // same as above
    blacklist(state, "loadfile"); // ^
    blacklist(state, "dofile"); // ^
    blacklist(state, "module"); // ^
    
    // we're keeping some os.* functions
    blacklist_table(state, "os", "execute");
    blacklist_table(state, "os", "exit");
    blacklist_table(state, "os", "getenv");
    blacklist_table(state, "os", "rename");
    blacklist_table(state, "os", "remove");
    blacklist_table(state, "os", "tmpname");
    blacklist_table(state, "os", "setlocale");
    
    // this function can be exploited to expose bytecode,
    // which may contain information normally not visible to Lua.
    blacklist_table(state, "string", "dump");

    blacklist(state, "collectgarbage"); // bad programming practise and unsafe
    
    return 0;
}

int util_setup(engine_inst* inst, lua_State* state) {
    lua_pushlightuserdata(state, &(inst->runtime_env));
    lua_setglobal(state, "__JNIENV");
    return 0;
}










