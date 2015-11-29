
/**

This is a utility class for Java objects that have a native implementation. This map
has unqiue keys and values, is NOT entirely thread safe, but safe when values are
created, used, and removed in the same thread context (multiple contexts can be used).

These maps can be created by choosing a unique integral value (one that is not used by
any other maps), and then calling pair_map_init().

When a thread ends, it is preferable to call pair_map_context_destroy() with
the macro PCONTEXT_NOCHECK. This will free data associated with that thread if it
was using any values stored by the map.

Allocations to buffers used for java objects and userdata are kept using pthread keys.

 */

#define _GNU_SOURCE

#include <jni.h>
#include <jni_md.h>

#include <stdint.h>

#ifndef PAIR_H_
#define PAIR_H_

/**
 * Used with pair_map_context_destroy()
 */
#define PCONTEXT_NOCHECK 1
#define PCONTEXT_IF_EMPTY 0

#define PAIR_MAP_DEBUG 1

typedef int pair_map; // this type is really just a key to lookup a datum

// key-value pairs
typedef struct pair_map_datum_ {
    
    jobject* java_pair; // java objects
    void** native_pair; // userdata
    
    size_t size; // amount of pairs in this struct
    
} pair_map_datum;

// collection context for each thread
typedef struct pair_map_collection_ {

    pair_map_datum** datums; // datums associated with this thread
    pair_map* maps; // maps associated with this thread

    size_t size; // amount of pairs in this struct
    
} pair_map_collection;

void pair_map_init(pair_map m);

void* pair_map_native(pair_map map, jobject java, JNIEnv* env);
jobject pair_map_java(pair_map map, void* native);
void pair_map_append(pair_map map, jobject java, void* native, JNIEnv* env);
void pair_map_rm_java(pair_map map, jobject java, JNIEnv* env);
void pair_map_rm_native(pair_map map, void* native, JNIEnv* env);
void pair_map_close(pair_map map);
void pair_map_rm_context(pair_map map, int (*predicate) (void* native, void* userdata), void* userdata);
void pair_map_context_destroy(uint8_t op);

#endif // PAIR_H_










