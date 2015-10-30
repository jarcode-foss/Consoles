
#include <jni.h>

#include <stdlib.h>
#include <stdint.h>
#include <string.h>

#include "pair.h"

#define LOCK(x, m) (*x)->MonitorEnter(x, m->lock)
#define UNLOCK(x, m) (*x)->MonitorExit(x, m->lock)
#define WAIT_FOR_WRITE(x, m) LOCK(x, m); UNLOCK(x, m)

// this implementation is free of memory barriers,
// since everything works on locks.

void pair_map_init(JNIEnv* env, pair_map* m) {
	m->java = &pair_map_java;
	m->native = &pair_map_native;
	m->rm_java = &pair_map_rm_java;
	m->rm_native = &pair_map_rm_native;
	m->close = &pair_map_close;
	m->append = &pair_map_append;
	jclass obj_type = (*env)->FindClass(env, PAIR_JAVA_LOCK_OBJECT);
	jmethodID mid = (*env)->GetMethodID(env, obj_type, "<init>", "()V");
	m->lock = (*env)->NewObject(env, obj_type, mid);
	m->lock = (*env)->NewGlobalRef(env, m->lock);
}

pair_map* pair_map_create(JNIEnv* env) {
	pair_map* m = malloc(sizeof(pair_map));
	pair_map_init(env, m);
	return m;
}

static int64_t lookup_idx(JNIEnv* env, pair_map* m, void* pair_set, uint64_t ptr, uint8_t type) {
	WAIT_FOR_WRITE(env, m);
	uint8_t valid = 0;
	int64_t t;
	for (t = 0; t < m->size; t++) {
		if (type ? ((void**) pair_set)[t] == (void*) ptr : ((jobject*) pair_set)[t] == (jobject) ptr) {
			valid = 1;
			break;
		}
	}
	if (!valid) return -1;
	else return t;
}
static void m_remove(JNIEnv* env, pair_map* m, int64_t t, int8_t locked) {
	if (!locked) LOCK(env, m);
	if (m->size > 0) {
		if (m->size == 1) {
			free(m->java_pair);
			free(m->native_pair);
		}
		else {
			int64_t newlen = (m->size - 1) * sizeof(void*);
			if (t != m->size) {
				jobject* java_ptr = &(m->java_pair[t]);
				void** native_ptr = &(m->native_pair[t]);
				int64_t chunkamt = m->size - (t + 1);
				memmove(java_ptr, java_ptr + 1, chunkamt);
				memmove(native_ptr, native_ptr + 1, chunkamt);
			}
			m->java_pair = realloc(m->java_pair, newlen);
			m->native_pair = realloc(m->native_pair, newlen);
		}
	}
	if (!locked) UNLOCK(env, m);
}
jobject pair_map_java(JNIEnv* env, pair_map* m, void* native) {
	WAIT_FOR_WRITE(env, m);
	uint8_t valid = 0;
	int64_t t;
	for (t = 0; t < m->size; t++) {
		if (m->native_pair[t] == native) {
			valid = 1;
			break;
		}
	}
	if (!valid) return 0;
	if (t >= 0) return m->java_pair[t];
	else return 0;
}
void* pair_map_native(JNIEnv* env, pair_map* m, jobject java) {
	WAIT_FOR_WRITE(env, m);
	uint8_t valid = 0;
	int64_t t;
	for (t = 0; t < m->size; t++) {
		if ((*env)->IsSameObject(env, java, m->java_pair[t])) {
			valid = 1;
			break;
		}
	}
	if (!valid) return 0;
	if (t >= 0) return m->native_pair[t];
	else return 0;
}
void pair_map_append(JNIEnv* env, pair_map* m, jobject java, void* native) {
	LOCK(env, m);
	if (m->size == 0) {
		m->java_pair = malloc(sizeof(jobject*));
		m->native_pair = malloc(sizeof(void**));
	}
	else {
		int64_t newlen = (m->size + 1) * sizeof(void*);
		m->java_pair = realloc(m->java_pair, newlen);
		m->native_pair = realloc(m->native_pair, newlen);
	}
	m->java_pair[m->size] = java;
	m->native_pair[m->size] = native;
	m->size++;
	UNLOCK(env, m);
}
void pair_map_rm_java(JNIEnv* env, pair_map* m, jobject java) {
	if (m->size == 0) return;
	int64_t t = lookup_idx(env, m, &(m->java_pair), (uint64_t) java, 0);
	if (t == -1) return;
	m_remove(env, m, t, 0);
}
void pair_map_rm_native(JNIEnv* env, pair_map* m, void* native) {
	if (m->size == 0) return;
	int64_t t = lookup_idx(env, m, &(m->native_pair), (uint64_t) native, 1);
	if (t == -1) return;
	m_remove(env, m, t, 0);
}
void pair_map_close(JNIEnv* env, pair_map* m) {
	(*env)->DeleteGlobalRef(env, m->lock);
	free(m->java_pair);
	free(m->native_pair);
	free(m);
}
void pair_map_rm(JNIEnv* env, pair_map* m, int (*predicate) (JNIEnv* env, void* ptr, void* userdata),
void* userdata) {
	LOCK(env, m);
	int64_t t;
	for (t = 0; t < m->size; t++) {
		if (predicate(env, m->native_pair[t], userdata)) {
			// can be optimized with mark & sweep
			m_remove(env, m, t, 1);
		}
	}
	UNLOCK(env, m);
}
