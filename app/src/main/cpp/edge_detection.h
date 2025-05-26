#ifndef EDGE_DETECTION_H
#define EDGE_DETECTION_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL
Java_com_example_edgevisionapp_MainActivity_processFrame(
        JNIEnv *env,
        jobject,
        jlong inputAddr,
        jlong outputAddr
);

#ifdef __cplusplus
}
#endif

#endif // EDGE_DETECTION_H
