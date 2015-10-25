
#ifndef ENGINE_H_
#define ENGINE_H_

typedef struct {
	lua_State* state;
	uint8_t closed;
} engine_inst;

void engine_setfunc(engine_inst* inst, JNIEnv* env, char* name, size_t name_len, jobject jfunc);

#endif // ENGINE_H_
