#include <jni.h>
#include <string>
#include <opencv2/opencv.hpp>
#include "edge_detection.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_edgevision_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "Hello from C++ with OpenCV!";
    return env->NewStringUTF(hello.c_str());
}
