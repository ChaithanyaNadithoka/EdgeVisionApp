#include <jni.h>
#include <opencv2/opencv.hpp>
#include <android/log.h>

#define LOG_TAG "EdgeDetection"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_edgevisionapp_MainActivity_processFrame(
        JNIEnv *env,
        jobject,
        jlong inputAddr,
        jlong outputAddr
) {
    cv::Mat &input = *(cv::Mat *) inputAddr;
    cv::Mat &output = *(cv::Mat *) outputAddr;

    cv::Mat gray, edges;
    cv::cvtColor(input, gray, cv::COLOR_RGBA2GRAY);
    cv::Canny(gray, edges, 80, 100);
    cv::cvtColor(edges, output, cv::COLOR_GRAY2RGBA);
}
