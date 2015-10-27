
#include <jni.h>

#include <stdlib.h>
#include <stdint.h>

#include "pair.h"

#define LOCK(x, m) (*x)->MonitorEnter(x, m->lock)
#define UNLOCK(x, m) (*x)->MonitorExit(x, m->lock)
#define WAIT_FOR_WRITE(x, m) LOCK(x, m); UNLOCK(x, m)

// this implementation is free of memory barriers,
// since everything works on locks.

void pair_map_init(JNIEnv* env, pair_map* m) {
void pair_map_init(JNIEnv* env, pair_map* m) {
	m->first = &pair_map_first;
	m->second = &pair_map_second;
	m->rm_first = &pair_map_rm_first;
	m->rm_second = &pair_map_rm_second;
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

static int64_t lookup(JNIEnv* env, pair_map* m, void** pair_set, void* ptr) {
	WAIT_FOR_WRITE(env, m);
	uint8_t valid = 0;
	int64_t t;
	for (t = 0; t < m->size; t++) {
		if (pair_set[t] == ptr) {
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
			free(m->first_pair);
			free(m->second_pair);
		}
		else {
			int64_t newlen = (m->size - 1) * sizeof(void*);
			if (t != m->size) {
				void* first_ptr = m->first_pair[t];
				void* second_ptr = m->second_pairt];
				int64_t chunkamt = m->size - (t + 1);
				memmove(first_ptr, first_ptr + 1, chunkamt);
				memmove(second_ptr, second_ptr + 1, chunkamt);
			}
			m->first_pair = realloc(m->first_pair, newlen);
			m->second_pair = realloc(m->second_pair, newlen);
		}
	}
	if (!locked) UNLOCK(env, m);
}
void* pair_map_first(JNIEnv* env, pair_map* m, void* second) {
	int64_t t = lookup(env, m, m->second_pair, second);
	if (t >= 0) return m->first_pair[t];
	else return 0;
}
void* pair_map_second(JNIEnv* env, pair_map* m, void* first) {
	int64_t t = lookup(env, m, m->first_pair, first);
	if (t >= 0) return m->second_pair[t];
	else return 0;
}
void pair_map_append(JNIEnv* env, pair_map* m, void* first, void* second) {
	LOCK(env, m);
	if (m->size == 0) {
		m->first_pair = malloc(sizeof(void*));
		m->second_pair = malloc(sizeof(void*));
	}
	else {
		int64_t newlen = (m->size + 1) * sizeof(void*);
		m->first_pair = realloc(m->first_pair, newlen);
		m->second_pair = realloc(m->second_pair, newlen);
	}
	m->first_pair[m->size] = first;
	m->second_pair[m->size] = second;
	m->size++;
	UNLOCK(env, m);
}
void pair_map_rm_first(JNIEnv* env, pair_map* m, void* first) {
	if (m->size == 0) return;
	int64_t t = lookup(m, &(m->first_pair), first);
	if (t == -1) return;
	m_remove(env, m, t, 0);
}
void pair_map_rm_second(JNIEnv* env, pair_map* m, void* second) {
	if (m->size == 0) return;
	int64_t t = lookup(m, &(m->second_pair), second);
	if (t == -1) return;
	m_remove(env, m, t, 0);
}
void pair_map_close(JNIEnv* env, pair_map* m) {
	(*env)->DeleteGlobalRef(env, m->lock);
	free(m->first_pair);
	free(m->second_pair);
	free(m);
}
void pair_map_rm(JNIEnv* env, pair_map* m, int (*predicate) (JNIEnv* env, void* ptr, void* userdata),
void* userdata, void** pair_set) {
	LOCK(env, m);
	int64_t t;
	for (t = 0; t < m->size; t++) {
		if (predicate(env, pair_set[t], userdata)) {
			// can be optimized with mark & sweep
			m_remove(env, m, t, 1);
		}
	}
	UNLOCK(env, m);
}
