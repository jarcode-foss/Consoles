#ifndef LUA_UTILS_H_
#define LUA_UTILS_H_

#include <lua.h>
#include <luajit.h> 
#include <lauxlib.h>
#include <lualib.h>

int util_blacklist(lua_State* state);
int util_setup(engine_inst* inst, lua_State* state);

#endif // LUA_UTILS_H_
