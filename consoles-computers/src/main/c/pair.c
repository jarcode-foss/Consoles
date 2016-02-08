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
#define VALIDATE_BUFFER(m, e) debug_validate_buffer(m, e)
#define VALIDATE_OBJECTS(m) debug_validate_objects(m)
#define VASSERT(e) assert(e)
#else
#define VALIDATE_BUFFER(m, e)
#define VALIDATE_OBJECTS(m)
#define VASSERT(e)
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
                        size_t t) // index of element to remove
{
    if (*size > 0) {
        if (*size == 1) {
            free(*first_pair);
            free(*second_pair);
            *first_pair = 0;
            *second_pair = 0;
        }
        else {
            VASSERT(t < *size && t >= 0);
            size_t newlen = ((*size) - 1) * sizeof(void*);
            VASSERT((*size * sizeof(void*)) - newlen == sizeof(void*));
            if (t != *size - 1) {
                // my (void**) casts are to ensure that pointers
                // are correctly being incremented
                void* first_ptr = (void**) *first_pair + t;
                void* second_ptr = (void**) *second_pair + t;
                size_t chunkamt = ((*size) - (t + 1)) * sizeof(void*);
                VASSERT(chunkamt < (*size * sizeof(void*)) && chunkamt > 0);
                memmove(first_ptr, (void**) first_ptr + 1, chunkamt);
                memmove(second_ptr, (void**) second_ptr + 1, chunkamt);
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

// get collection (datum from thread context)
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

// get datum specific to the given pair map
static inline pair_map_datum* get_datum(pair_map map) {
    pair_map_collection* col = get_collection();
    int64_t idx = lookup_idx(&(col->maps), col->size, &map, COMPARE_PAIR_MAP, 0);
    
    if (idx == -1) {
        return 0;
    } else return col->datums[idx];
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

#if PAIR_MAP_DEBUG > 0
static void debug_validate_objects(pair_map map) {
    pair_map_datum* m = get_datum(map);
    assert(m);
    
    printf("C MAP: validating %d address pairs\n", (int) m->size);
    int64_t t;
    for (t = 0; t < m->size; t++) {
        // we just check if the jobject is a bad pointer
        assert(*(void**) (m->java_pair[t]));
    }
}

static void debug_validate_buffer(pair_map map, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    assert(m);
    
    printf("C MAP: validating %d sets\n", (int) m->size);
    int64_t t;
    for (t = 0; t < m->size; t++) {
        
        // assert that the jobject is valid, we do this
        // by calling some JNI function with it
        jclass c = (*env)->GetObjectClass(env, m->java_pair[t]);
        // The JVM will SIGABRT or complain about the ref and return null
        assert(c);
        // if the assertion passed, cleanup the local reference
        (*env)->DeleteLocalRef(env, c);
        // assert that the engine value pointed to is valid.
        assert(ENGINE_ASSERT_VALUE(m->native_pair[t]));

        ASSERTEX(env);
    }
}
#endif // PAIR_MAP_DEBUG

int pair_map_context_init(pair_map map) {
    pair_map_collection* col = get_collection();
    int64_t idx = lookup_idx(&(col->maps), col->size, &map, COMPARE_PAIR_MAP, 0);

    if (idx != -1) return 1;
    
    pair_map_datum* m = malloc(sizeof(pair_map_datum));
    memset(m, 0, sizeof(pair_map_datum));
        
    set_extend(&(col->maps), &(col->datums), &(col->size));
    col->maps[col->size - 1] = map;
    col->datums[col->size - 1] = m;

    return 0;
}

void* pair_map_index_native(pair_map map, size_t index) {
    pair_map_datum* m = get_datum(map);
    return m->native_pair[index];
}

jobject pair_map_index_java(pair_map map, size_t index) {
    pair_map_datum* m = get_datum(map);
    return m->java_pair[index];
}

size_t pair_map_context_size(pair_map map) {
    return get_datum(map)->size;
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
    if (!m) {
        return 0;
    }
    
    int64_t t = lookup_idx(&(m->native_pair), m->size, &native, COMPARE_USERDATA, 0);
    
    if (t >= 0) return m->java_pair[t];
    else return PAIR_MAP_OK;
}
void* pair_map_native(pair_map map, jobject java, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    if (!m) {
        return 0;
    }
    
    int64_t t = lookup_idx(&(m->java_pair), m->size, &java, COMPARE_JAVA_OBJ, env);
    
    if (t >= 0) return m->native_pair[t];
    else return PAIR_MAP_OK;
}
int pair_map_append(pair_map map, jobject java, void* native, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    if (!m) {
        return PAIR_MAP_MISSING_DATUM;
    }
    
    set_extend(&(m->java_pair), &(m->native_pair), &(m->size));
    m->java_pair[m->size - 1] = (*env)->NewGlobalRef(env, java);
    m->native_pair[m->size - 1] = native;
    
    VALIDATE_BUFFER(map, env);

    return PAIR_MAP_OK;
}
int pair_map_rm_java(pair_map map, jobject java, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    if (!m) {
        return PAIR_MAP_MISSING_DATUM;
    }
    
    if (m->size == 0) return 2;
    int64_t t = lookup_idx(&(m->java_pair), m->size, &java, COMPARE_JAVA_OBJ, env);
    if (t == -1) return 2;
    set_remove(&(m->java_pair), &(m->native_pair), &(m->size), t);
    
    VALIDATE_BUFFER(map, env);

    return PAIR_MAP_OK;
}
int pair_map_rm_native(pair_map map, void* native, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    if (!m) {
        return PAIR_MAP_MISSING_DATUM;
    }
    
    if (m->size == 0) return 2;
    int64_t t = lookup_idx(&(m->native_pair), m->size, &native, COMPARE_USERDATA, 0);
    if (t == -1) return 2;
    
    (*env)->DeleteGlobalRef(env, m->java_pair[t]);
    
    set_remove(&(m->java_pair), &(m->native_pair), &(m->size), t);
    
    VALIDATE_BUFFER(map, env);

    return PAIR_MAP_OK;
}
int pair_map_close(pair_map map) {
    
    pair_map_collection* col = get_collection();
    int64_t idx = lookup_idx(&(col->maps), col->size, &map, COMPARE_PAIR_MAP, 0);
    pair_map_datum* m = col->datums[idx];
    
    if (!m) {
        return PAIR_MAP_MISSING_DATUM;
    }

    // remove the datum from the mappings
    set_remove(&(col->maps), &(col->datums), &(col->size), idx);

    // free the datum and its members
    free_datum(m);

    // if we are closing this map and no other map is being used in
    // this context, then destroy the context.
    pair_map_context_destroy(PCONTEXT_IF_EMPTY);

    return PAIR_MAP_OK;
}
// this function carries a lot of emotions with it, mostly over my
// frustration for not thinking about implicit integer type promotion.
int pair_map_rm_context(pair_map map, int (*predicate) (void* ptr, void* userdata), void* userdata, JNIEnv* env) {
    pair_map_datum* m = get_datum(map);
    
    if (!m) {
        return PAIR_MAP_MISSING_DATUM;
    }
    
    size_t t = m->size;
    if (!t) return PAIR_MAP_OK;
    
    size_t idxs[t];
    size_t i = 0;
    // backwards iterate, because the right-most elements of t will shift left.
    while (1) {
        t--;
        if (predicate(m->native_pair[t], userdata)) {
            idxs[i] = t;
            i++;
        }
        if (t == 0) break;
    }
    // for now we just remove them normally, one by one. We could use
    // a deframenting approach to this instead.
    for (t = 0; t < i; t++) {
        (*env)->DeleteGlobalRef(env, m->java_pair[idxs[t]]);
        
        set_remove(&(m->java_pair), &(m->native_pair), &(m->size), idxs[t]);
        // assert that all the jobjects point to valid addresses
        VALIDATE_OBJECTS(map);
    }
    // validate after removing all values, since the predicate may free pointers
    // that are passed to it
    VALIDATE_BUFFER(map, env);

    return PAIR_MAP_OK;
}
