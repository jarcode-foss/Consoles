
#include <jni.h>

#include <stdlib.h>
#include <stdio.h>

#include "engine.h"

static jclass class_lobj;
static jmethodID lobj_constructor;
static jfieldID id_odata, id_orefs;

jobject ln_getref(jobject ln_obj, size_t idx) {
    
}
