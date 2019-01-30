#pragma once

#include <jni.h>

#ifdef __GNUC__
#define CONNECTOR_EXPORT __attribute__ ((visibility("default"))) JNIEXPORT
#else
#define CONNECTOR_EXPORT JNIEXPORT
#endif

#ifdef MSC_VER
#define CONNECTOR_IMPORT __declspec(dllimport)
#else
#define CONNECTOR_IMPORT
#endif
