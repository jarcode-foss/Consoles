#include <stdint.h>

#ifndef PAIR_H_
#define PAIR_H_

#define PAIR_JAVA_LOCK_OBJECT "java/lang/Object"

// this is a pair map that is synchronized on a java object (through the JNI)
// this map should be used for storing native class members in the C implementations of a class
typedef struct pair_map_ {
	
	// volative pair fields and size
	// (volatile pointer to volatile pointer)
	void* volatile* volatile first_pair;
	void* volatile* volatile second_pair;
	volatile uint64_t size;
	
	// java object used for locking
	jobject lock;
	
	// function pointers
	void* (*second) (JNIEnv* env, pair_map* map, void* first);
	void* (*first) (JNIEnv* env, pair_map* map, void* second);
	void (*append) (JNIEnv* env, pair_map* map, void* first, void* second);
	void (*rm_first) (JNIEnv* env, pair_map* map, void* first);
	void (*rm_second) (JNIEnv* env, pair_map* map, void* second);
	void (*close) (JNIEnv* env, pair_map* map);
	void (*rm)
	(JNIEnv* env, pair_map* m, int (*predicate) (void* ptr, void* userdata), void* userdata, void** pair_set);
} pair_map;

pair_map* pair_map_create();
void pair_map_init(JNIEnv* env, pair_map* m);

void* pair_map_second(JNIEnv* env, pair_map* map, void* first);
void* pair_map_first(JNIEnv* env, pair_map* map, void* second);
void pair_map_append(JNIEnv* env, pair_map* map, void* first, void* second);
void pair_map_rm_first(JNIEnv* env, pair_map* map, void* first);
void pair_map_rm_second(JNIEnv* env, pair_map* map, void* second);
void pair_map_close(JNIEnv* env, pair_map* map);
void pair_map_rm
(JNIEnv* env, pair_map* m, int (*predicate) (void* ptr, void* userdata), void* userdata, void** pair_set) 

#endif // PAIR_H_
