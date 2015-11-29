#include <jni.h>
#include <jni_md.h>

#include <stdlib.h>
#include <stdint.h>
#include <stdio.h>

#include <string.h>

#include <pthread.h>

#include "pair.h"

#define COMPARE_JAVA_OBJ 0
#define COMPARE_USERDATA 1
#define COMPARE_PAIR_MAP 2

#if PAIR_MAP_DEBUG > 0
#include <assert.h>
#include "engine.h"
#define VALIDATE_BUFFER(m) debug_validate_buffer(m);
#else
#define VALIDATE_BUFFER(m)
#endif // PAIR_MAP_DEBUG

#define set_remove(a, b, c, d) _set_remove((void**) a, (void**) b, c, d)
#define set_extend(a, b, c) _set_extend((void**) a, (void**) b, c)

static pthread_key_t key;
static pthread_once_t key_once = PTHREAD_ONCE_INIT;

/**
 * Looks up the index of an element in a buffer, with a certain comparison type. If the
 * element does not exist, -1 is returned.
 */
static int64_t lookup_idx(void* pair_set, // pointer to buffer to use
                          size_t size, // buffer size
                          void* ptr, // pointer to element to compare
                          uint8_t type, // comparison type
                          JNIEnv* env) // JNI environment, if type == COMPARE_JAVA_OBJ
{
    uint8_t valid = 0;
    int64_t t;
    for (t = 0; t < size; t++) {
        int8_t flag;
        switch (type) {
        case COMPARE_JAVA_OBJ: // java object comparison
            flag = (*env)->IsSameObject(env, (*(jobject**) pair_set)[t], *(jobject*) ptr);
            break;
        case COMPARE_USERDATA: // userdata comparison
            flag = (*(void***) pair_set)[t] == *(void**) ptr;
            break;
        case COMPARE_PAIR_MAP: // pair_map comparison
            flag = (*(pair_map**) pair_set)[t] == *(pair_map*) ptr;
            break;
        }
        if (flag) {
            valid = 1;
            break;
        }
    }
    if (!valid) return -1;
    else return t;
}

/**
 * Removes element 't' from two buffers, and shifts the right-most values down in its place.
 * The buffers' original position may change due to realloc(). If size is equal to 1, then
 * the buffers will be free()'d.
 */
static void _set_remove(void** first_pair, // pointer to first buffer
                       void** second_pair, // pointer to second buffer
                       size_t* size, // pointer to buffer size
                       int64_t t) // index of element to remove
{
    if (*size > 0) {
        if (*size == 1) {
            free(*first_pair);
            free(*second_pair);
            *first_pair = 0;
            *second_pair = 0;
        }
        else {
            size_t newlen = ((*size) - 1) * sizeof(void*);
            if (t != *size - 1) {
                void* first_ptr = (*first_pair) + t;
                void* second_ptr = (*second_pair) + t;
                size_t chunkamt = ((*size) - (t + 1)) * sizeof(void*);
                memmove(first_ptr, first_ptr + 1, chunkamt);
                memmove(second_ptr, second_ptr + 1, chunkamt);
            }
            *first_pair = realloc(*first_pair, newlen);
            *second_pair = realloc(*second_pair, newlen);
        }
        (*size)--;
    }
}

/**
 * Extends the size of two buffers by one, incrementing size. If size is equal to zero,
 * then the buffers with be allocated with malloc().
 */
static void _set_extend(void** first_pair, // pointer to first buffer
                       void** second_pair, // pointer to second buffer
                       size_t* size) // pointer to buffer size
{
    if (*size == 0) {
        *first_pair = malloc(sizeof(void*));
        *second_pair = malloc(sizeof(void*));
    }
    else {
        int64_t newlen = ((*size) + 1) * sizeof(void*);
        *first_pair = realloc(*first_pair, newlen);
        *second_pair = realloc(*second_pair, newlen);
    }
    (*size)++;
}

static inline void setup_abort(int code, const char* func) {
    fprintf(stderr, "%s(...) returned error code: %d", func, code);
    exit(EXIT_FAILURE);
}

static inline void setup() {
    int ret = pthread_key_create(&key, 0);
    if (ret) {
        setup_abort(ret, "pthread_key_create");
    }
}

static inline pair_map_collection* get_collection() {
    (void) pthread_once(&key_once, &setup);
    pair_map_collection* ptr = pthread_getspecific(key);
    
    if (!ptr) {
        ptr = malloc(sizeof(pair_map_collection));
        memset(ptr, 0, sizeof(pair_map_collection));
        (void) pthread_setspecific(key, ptr);
    }
    
    return ptr;
}

static inline pair_map_datum* get_datum(pair_map m) {
    pair_map_collection* col = get_collection();
    int64_t idx = lookup_idx(&(col->maps), col->size, &m, COMPARE_PAIR_MAP, 0);
    pair_map_datum* datum = col->datums[idx];
    if (!datum) {
        datum = malloc(sizeof(pair_map_datum));
        memset(datum, 0, sizeof(pair_map_datum));
    }
    return datum;
}

static inline void free_collection(pair_map_collection* col) {
    if (col->size) {
        free(col->datums);
        free(col->maps);
    }
    free(col);
}

static inline void free_datum(pair_map_datum* m) { 
    if (m->size) {
        free(m->java_pair);
        free(m->native_pair);
    }   
    free(m);
}

static void debug_validate_buffer(pair_map map) {
    pair_map_datum* m = get_datum(map);
    
    printf("C MAP: validating %d sets\n", (int) m->size);
    int64_t t;
    for (t = 0; t < m->size; t++) {
        
        // This will segfault (corrupt memory address) if the
        // buffer(s) were corrutped.
        void* mem1 = *(void**) (m->java_pair[t]);
        void* mem2 = *(void**) (m->native_pair[t]);
        // assert that the jobject doesn't point to null
        assert(mem1);
        // assert that the engine value pointed to is valid.
        assert(ENGINE_ASSERT_VALUE(&mem2));
    }
}

void pair_map_init(pair_map map) {
    // create datum for this thread
    pair_map_datum* m = malloc(sizeof(pair_map_datum));
    memset(m, 0, sizeof(pair_map_datum));
    
    pair_map_collection* col = get_collection();
    set_extend(&(col->maps), &(col->datums), &(col->size));
    col->maps[col->size - 1] = map;
    col->datums[col->size - 1] = m;
}

void pair_map_context_destroy(uint8_t op) {
    pair_map_collection* col = get_collection();
    int64_t t;
    switch (op) {
    case PCONTEXT_IF_EMPTY:
        if (col->size == 0) {
            free_collection(col);
        }
        break;
    case PCONTEXT_NOCHECK:
        for (t = 0; t < col->size; t++) {
            free_datum(col->datums[t]);
        }
        free_collection(col);
        break;
    }
}

jobject pair_map_java(pair_map map, void* native) {
    pair_map_datum* m = get_datum(map);
    
    int64_t t = lookup_idx(&(m->native_pair), m->size, &native, COMPARE_USERDATA, 0);
    
    if (t >= 0) return m->java_pair[t];
    else return 0;
}
void* pair_map_native(pair_map map, jobject java, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    
    int64_t t = lookup_idx(&(m->java_pair), m->size, &java, COMPARE_JAVA_OBJ, env);
    
    if (t >= 0) return m->native_pair[t];
    else return 0;
}
void pair_map_append(pair_map map, jobject java, void* native, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    
    set_extend(&(m->java_pair), &(m->native_pair), &(m->size));
    m->java_pair[m->size - 1] = (*env)->NewGlobalRef(env, java);
    m->native_pair[m->size - 1] = native;
    
    VALIDATE_BUFFER(map);
}
void pair_map_rm_java(pair_map map, jobject java, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    
    if (m->size == 0) return;
    int64_t t = lookup_idx(&(m->java_pair), m->size, &java, COMPARE_JAVA_OBJ, env);
    if (t == -1) return;
    set_remove(&(m->java_pair), &(m->native_pair), &(m->size), t);
    
    VALIDATE_BUFFER(map);
}
void pair_map_rm_native(pair_map map, void* native, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    
    if (m->size == 0) return;
    int64_t t = lookup_idx(&(m->native_pair), m->size, &native, COMPARE_USERDATA, 0);
    if (t == -1) return;
    
    (*env)->DeleteGlobalRef(env, m->java_pair[t]);
    
    set_remove(&(m->java_pair), &(m->native_pair), &(m->size), t);
    
    VALIDATE_BUFFER(map);
}
void pair_map_close(pair_map map) {
    
    pair_map_collection* col = get_collection();
    int64_t idx = lookup_idx(&(col->maps), col->size, &map, COMPARE_PAIR_MAP, 0);
    pair_map_datum* m = col->datums[idx];

    // remove the datum from the mappings
    set_remove(&(col->maps), &(col->datums), &(col->size), idx);

    // free the datum and its members
    free_datum(m);

    // if we are closing this map and no other map is being used in
    // this context, then destroy the context.
    pair_map_context_destroy(PCONTEXT_IF_EMPTY);
}
void pair_map_rm_context(pair_map map, int (*predicate) (void* ptr, void* userdata), void* userdata) {
    pair_map_datum* m = get_datum(map);
    
    int64_t t;
    uint64_t s = m->size; // make copy, since m->size will change
    if (!s) return;
    
    int64_t idxs[s];
    int64_t i = 0;
    // backwards iterate, because the right-most elements of t will shift left.
    for (t = s - 1; t >= 0; t--) {
        if (predicate(m->native_pair[t], userdata)) {
            idxs[i] = t;
            i++;
        }
    }
    // for now we just remove them normally, one by one. We could use
    // a deframenting approach to this instead.
    for (t = 0; t < i; t++) {
        set_remove(&(m->java_pair), &(m->native_pair), &(m->size), idxs[t]);
        VALIDATE_BUFFER(map);
    }
}
