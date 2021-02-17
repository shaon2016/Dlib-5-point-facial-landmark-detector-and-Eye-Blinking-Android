#include <jni.h>
#include <string>
#include <iostream>
#include <mutex>

// Dlib headers
#include <dlib/image_processing/frontal_face_detector.h>
#include <dlib/image_processing.h>
#include "dlib/opencv.h"
#include <dlib/pixel.h>

// Open CV
#include <opencv2/opencv.hpp>
#include "opencv2/imgproc/imgproc_c.h"

// Android
#include <android/log.h>

using namespace dlib;
using namespace std;

#define LOG_TAG "native-lib"
#define LOGD(...) \
  ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

#define JNI_METHOD(NAME) \
    Java_com_shaon2016_dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark_Native_##NAME

#define KERNEL_SIZE 5 // 3, 5, 7, 9

#define NV21 17
#define YV12 842094169
#define YUV_420_888 35
#define PYRAMIDS 3
#define MAX_FRAME_COUNT 5

// global variables:
shape_predictor shapePredictor;
std::mutex _mutex;
int imageFormat = NV21;

void rotateMat(cv::Mat &mat, int rotation);

extern "C" JNIEXPORT void JNICALL
JNI_METHOD(loadModel)(JNIEnv *env, jobject, jstring detectorPath) {
    try {
        const char *path = env->GetStringUTFChars(detectorPath, JNI_FALSE);

        _mutex.lock();
        // cause the later initialization of the tracking
//        LK::isTracking = false;

        // load the shape predictor
        dlib::deserialize(path) >> shapePredictor;
        _mutex.unlock();

        env->ReleaseStringUTFChars(detectorPath, path); //free mem
        LOGD("JNI: model loaded");

    } catch (dlib::serialization_error &e) {
        LOGD("JNI: failed to model -> %s", e.what());
    }
}


extern "C" JNIEXPORT int JNICALL
JNI_METHOD(detectLandmarkARGB)(
        JNIEnv *env, jobject, jintArray pixels, jint width,
        jint height) {

    // Working with image
    array2d<unsigned char> img;

    jint *p = env->GetIntArrayElements(pixels, 0);
    cv::Mat mFrame = cv::Mat(height, width, CV_8UC4,  p).clone();
    // the next only is a extra example to gray convertion:
    cv::Mat mOut;
    // to grayscale
    cv::cvtColor(mFrame, mOut, cv::COLOR_RGB2GRAY);

    // convert mat image to dlib image
    dlib::assign_image(img, dlib::cv_image<unsigned char>(mOut));


   // pyramid_up(img);
    // end of working with image

//     We need a face detector.  We will use this to get bounding boxes for
//     each face in an image.
    frontal_face_detector detector = get_frontal_face_detector();
    // Now tell the face detector to give us a list of bounding boxes
    // around all the faces in the image.
    std::vector<rectangle> dets = detector(img);

    env->ReleaseIntArrayElements(pixels, p, 0);

    return dets.size();
}




