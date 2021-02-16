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

void convertNV21ToArray2d(JNIEnv *env, dlib::array2d<dlib::rgb_pixel> &out,
                          jbyteArray data, jint width, jint height);

jbyteArray convertBitmapArrayToNV21(JNIEnv *env, jintArray argbPixels, jint width, jint height);


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
JNI_METHOD(detectLandmark)(
        JNIEnv *env, jobject, jintArray pixels, jint width,
        jint height) {


    array2d<rgb_pixel> img;

    try {
        jbyteArray nv21ByteArray = convertBitmapArrayToNV21(env, pixels, width, height);
        convertNV21ToArray2d(env, img, nv21ByteArray, width, height);

        LOGD("JNI: Conversion successful image }");
    } catch (exception &e) {
        LOGD("JNI: failed to convert image -> %s", e.what());
    }


    return 0;
}

/**
 * Convert an rgb pixel image to gray image using RGB 8888 image pixel
 * */
extern "C" JNIEXPORT jintArray JNICALL
JNI_METHOD(imageToGrayScale)(
        JNIEnv *env, jobject, jintArray p) {
    jint *pixels = env->GetIntArrayElements(p, 0);
    int len = env->GetArrayLength(p);

    jintArray na = env->NewIntArray(len);
    jint *narr = env->GetIntArrayElements(na, NULL);

    for (int i = 0; i < len; i++) {
        //getting individual color values from each pixel
        int A = (pixels[i] >> 24) & 0xFF;
        int R = (pixels[i] >> 16) & 0xFF;
        int G = (pixels[i] >> 8) & 0xFF;
        int B = pixels[i] & 0xFF;

        //averaging Red, Green and Blue value to get gray scale value
        int gray = (R + G + B) / 3;

        //assign same gray value to Red, Green and Blue.
        //alpha value is unchanged
        pixels[i] = (A << 24) | (gray << 16) | (gray << 8) | gray;

        narr[i] = pixels[i];
    }

    env->ReleaseIntArrayElements(p, pixels, 0);
    env->ReleaseIntArrayElements(na, narr, 0);

    return na;
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
}


void downScaleNV21(JNIEnv *env, jobject obj,
                   jbyteArray data, jint width, jint height) {
    int width_ds = width / 2;
    int height_ds = height / 2;
    jbyte *yuv = env->GetByteArrayElements(data, 0);
    jbyte *yuv_ds = new jbyte[((width / 2) * (height / 2) * 3) / 2];
    int frameSize = width * height;
    int frameSize_ds = width_ds * height_ds;

    int y1, y2, y3, y4;
    int u, v;
    int uvIndex, uvIndex1, uvIndex2, uvIndex3, uvIndex4;

    for (int row = 0, row_ds = 0; row < height; row = +4, row_ds += 2) {
        for (int column = 0, column_ds = 0; column < width; column = +4, column_ds += 2) {

            //      0     1     2     3
            //    ______________________
            // 0 | y1a | y1b | y2a | y2b |
            //   |-----+-----|-----+-----|          ____ ____
            // 1 | y1c | y1d | y2c | y2d |         | y1 | y2 |
            //    -----------+-----------     ->    ----+----
            // 2 | y3a | y3b | y4a | y4b |         | y3 | y4 |
            //   |-----+-----|-----+-----|          ---- ----
            // 3 | y3c | y3d | y4c | y4d |
            //    ----------- -----------

            y1 = (yuv[row * width + column] +               // y1a
                  yuv[row * width + (column + 1)] +         // y1b
                  yuv[(row + 1) * width + column] +         // y1c
                  yuv[(row + 1) * width + (column + 1)]) / 4; // y1d

            y2 = (yuv[row * width + (column + 2)] +         // y2a
                  yuv[row * width + (column + 3)] +         // y2b
                  yuv[(row + 1) * width + (column + 2)] +   // y2c
                  yuv[(row + 1) * width + (column + 3)]) / 4; // y2d

            y3 = (yuv[(row + 2) * width + column] +         // y3a
                  yuv[(row + 2) * width + (column + 1)] +   // y3b
                  yuv[(row + 3) * width + column] +         // y3c
                  yuv[(row + 3) * width + (column + 1)]) / 4; // y3d

            y4 = (yuv[(row + 2) * width + (column + 2)] +   // y4a
                  yuv[(row + 2) * width + (column + 3)] +   // y4b
                  yuv[(row + 3) * width + (column + 2)] +   // y4c
                  yuv[(row + 3) * width + (column + 3)]) / 4; // y4d

            uvIndex1 = frameSize + (row >> 1) * width + (column & ~1);
            uvIndex2 = frameSize + (row >> 1) * width + ((column + 2) & ~1);
            uvIndex3 = frameSize + ((row + 2) >> 1) * width + (column & ~1);
            uvIndex4 = frameSize + ((row + 2) >> 1) * width + ((column + 2) & ~1);
            uvIndex = frameSize_ds + (row_ds >> 1) * width_ds + (column_ds & ~1);

            v = (yuv[uvIndex1] + yuv[uvIndex2] + yuv[uvIndex3] + yuv[uvIndex4]) / 4;
            u = (yuv[uvIndex1 + 1] + yuv[uvIndex2 + 1] + yuv[uvIndex3 + 1] + yuv[uvIndex4 + 1]) / 4;

            yuv_ds[row * width + column] = (jbyte) y1;
            yuv_ds[row * width + (column + 1)] = (jbyte) y2;
            yuv_ds[(row + 1) * width + column] = (jbyte) y3;
            yuv_ds[(row + 1) * width + (column + 1)] = (jbyte) y4;
            yuv_ds[uvIndex] = (jbyte) v;
            yuv_ds[uvIndex + 1] = (jbyte) u;
        }
    }
}


jbyteArray convertBitmapArrayToNV21(JNIEnv *env, jintArray argbPixels, jint width, jint height) {


//    jbyteArray yuv = env->NewByteArray(width * ((height) * (3 / 2)));
    jbyteArray yuv = env->NewByteArray(width * height * 3 / 2);
    jbyte *yuv420sp = env->GetByteArrayElements(yuv, NULL);

    jint *argb = env->GetIntArrayElements(argbPixels, 0);

    int frameSize = width * height;
    int yIndex = 0;
    int uvIndex = frameSize;
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
                yuv420sp[uvIndex++] = ((V < 0) ? 0 : ((V > 255) ? 255 : V));
                yuv420sp[uvIndex++] = ((U < 0) ? 0 : ((U > 255) ? 255 : U));
            }
            index++;
        }
    }

    env->ReleaseByteArrayElements(yuv, yuv420sp, 0);
    env->ReleaseIntArrayElements(argbPixels, argb, 0);

    return yuv;
}