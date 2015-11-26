#include <stdint.h>

#ifndef PAIR_H_
#define PAIR_H_

#define PAIR_JAVA_LOCK_OBJECT "java/lang/Object"

// locking and unlocking macros
#define LOCK(x, m) (*x)->MonitorEnter(x, m->lock)
#define UNLOCK(x, m) (*x)->MonitorExit(x, m->lock)
#define WAIT_FOR_WRITE(x, m) LOCK(x, m); UNLOCK(x, m)

// this is a pair map that is synchronized on a java object (through the JNI)
// this map should be used for storing native class members in the C implementations of a class
typedef struct pair_map_ {
	
	jobject* java_pair;
	void** native_pair;
	volatile uint64_t size;
	
	// java object used for locking
	volatile jobject lock;
	
	// function pointers
	void* (*native) (JNIEnv* env, struct pair_map_* map, jobject java);
	jobject (*java) (JNIEnv* env, struct pair_map_* map, void* native);
	void (*append) (JNIEnv* env, struct pair_map_* map, jobject java, void* native);
	void (*rm_java) (JNIEnv* env, struct pair_map_* map, jobject java);
	void (*rm_native) (JNIEnv* env, struct pair_map_* map, void* native);
	void (*close) (JNIEnv* env, struct pair_map_* map);
	void (*rm)
	(JNIEnv* env, struct pair_map_* m, int (*predicate) (JNIEnv* env, void* ptr, void* userdata), void* userdata);
} pair_map;

pair_map* pair_map_create();
void pair_map_init(JNIEnv* env, pair_map* m);

void* pair_map_native(JNIEnv* env, pair_map* map, jobject java);
jobject pair_map_java(JNIEnv* env, pair_map* map, void* native);
void pair_map_append(JNIEnv* env, pair_map* map, jobject java, void* native);
void pair_map_rm_java(JNIEnv* env, pair_map* map, jobject java);
void pair_map_rm_native(JNIEnv* env, pair_map* map, void* native);
void pair_map_close(JNIEnv* env, pair_map* map);
void pair_map_rm(JNIEnv* env, pair_map* m, int (*predicate) (JNIEnv* env, void* ptr, void* userdata),
void* userdata);

#endif // PAIR_H_
