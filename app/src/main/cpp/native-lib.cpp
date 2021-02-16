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

// Android
#include <android/log.h>

using namespace dlib;
using namespace std;

#define LOG_TAG "native-lib"
#define LOGD(...) \
  ((void)__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__))

#define JNI_METHOD(NAME) \
    Java_com_shaon2016_dlibrealtimefacedetectionandeyeblinkingwith5pointfaciallandmark_Native_##NAME

// global variables:
shape_predictor shapePredictor;
std::mutex _mutex;

void convertNV21ToArray2d(JNIEnv *env, dlib::array2d<dlib::rgb_pixel> &out,
                          jbyteArray data, jint width, jint height);

jbyteArray convertBitmapPixelToNV21(JNIEnv *env, jintArray argbPixels, jint width, jint height);


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

extern "C" JNIEXPORT jint JNICALL
JNI_METHOD(detectLandmark)(
        JNIEnv *env, jobject ob, jintArray pixels, jint width,
        jint height) {

    array2d<rgb_pixel> img;

    try {
        jbyteArray nv21ByteArray = convertBitmapPixelToNV21(env, pixels, width, height);

        convertNV21ToArray2d(env, img, nv21ByteArray, width, height);

        LOGD("JNI: Conversion successful image }");
    } catch (exception &e) {
        LOGD("JNI: failed to convert image -> %s", e.what());
    }

    pyramid_up(img);

    // We need a face detector.  We will use this to get bounding boxes for
    // each face in an image.
    frontal_face_detector detector = get_frontal_face_detector();
    // Now tell the face detector to give us a list of bounding boxes
    // around all the faces in the image.
    std::vector<rectangle> dets = detector(img);

//    dlib::rectangle region(0, 0, 300, 300);
//    dlib::full_object_detection points = shapePredictor(img, region);
//    LOGD("JNI: Number of points detected -> %s", points.num_parts());

    return dets.size();
}

void convertNV21ToArray2d(JNIEnv *env, dlib::array2d<dlib::rgb_pixel> &out,
                          jbyteArray data, jint width, jint height) {
    jbyte *yuv = env->GetByteArrayElements(data, 0);
    int frameSize = width * height;
    int y, u, v, uvIndex;
    int r, g, b;

    out.set_size((long) height, (long) width);

    for (int row = 0; row < height; row++) {
        for (int column = 0; column < width; column++) {
            uvIndex = frameSize + (row >> 1) * width + (column & ~1);
            y = 0xff & yuv[row * width + column] - 16;
            v = 0xff & yuv[uvIndex] - 128;
            u = 0xff & yuv[uvIndex + 1] - 128;

            y = y < 0 ? 0 : 1164 * y;

            r = y + 1596 * v;
            g = y - 813 * v - 391 * u;
            b = y + 2018 * u;

            out[row][column].red = (unsigned char) (r < 0 ? 0 : r > 255000 ? 255 : r / 1000);
            out[row][column].green = (unsigned char) (g < 0 ? 0 : g > 255000 ? 255 : g / 1000);
            out[row][column].blue = (unsigned char) (b < 0 ? 0 : b > 255000 ? 255 : b / 1000);
        }
    }

    env->ReleaseByteArrayElements(data, yuv, 0);
}


jbyteArray convertBitmapPixelToNV21(JNIEnv *env, jintArray argbPixels, jint width, jint height) {

//    jbyteArray yuv = env->NewByteArray(
//            height * width + 2 * (int) ceil(height / 2.0) * (int) ceil(width / 2.0));
    jbyteArray yuv = env->NewByteArray((width * height * 3) / 2);
    jbyte *yuv420sp = env->GetByteArrayElements(yuv, NULL);

    jint *argb = env->GetIntArrayElements(argbPixels, 0);

    int frameSize = width * height;
    int yIndex = 0;
    int uIndex = frameSize;
    int vIndex = frameSize + ((env->GetArrayLength(yuv) - frameSize) / 2);
    int a, R, G, B, Y, U, V;
    int index = 0;

    for (int j = 0; j < height; j++) {
        for (int i = 0; i < width; i++) {

            a = (argb[index] & 0xff000000) >> 24; // a is not used obviously
            R = (argb[index] & 0xff0000) >> 16;
            G = (argb[index] & 0xff00) >> 8;
            B = (argb[index] & 0xff) >> 0;

            // well known RGB to YUV algorithm

            Y = ((66 * R + 129 * G + 25 * B + 128) >> 8) + 16;
            U = ((-38 * R - 74 * G + 112 * B + 128) >> 8) + 128;
            V = ((112 * R - 94 * G - 18 * B + 128) >> 8) + 128;

            // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
            //    meaning for every 4 Y pixels there are 1 V and 1 U.  Note the sampling is every other
            //    pixel AND every other scanline.
            yuv420sp[yIndex++] = ((Y < 0) ? 0 : ((Y > 255) ? 255 : Y));
            if (j % 2 == 0 && index % 2 == 0) {
                yuv420sp[uIndex++] = ((U < 0) ? 0 : ((U > 255) ? 255 : U));
                yuv420sp[vIndex++] = ((V < 0) ? 0 : ((V > 255) ? 255 : V));
            }

            index++;
        }
    }

    env->ReleaseByteArrayElements(yuv, yuv420sp, 0);
    env->ReleaseIntArrayElements(argbPixels, argb, 0);

    return yuv;
}

