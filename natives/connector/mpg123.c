#include <jni.h>
#include <aacdecoder_lib.h>

#ifdef MSC_VER
#define IMPORT __declspec(dllimport)
#else
#define IMPORT
#endif

IMPORT int mpg123_init(void);
IMPORT void* mpg123_new(const char* decoder, int *error);
IMPORT void mpg123_delete(void *mh);
IMPORT int mpg123_open_feed(void *mh);
IMPORT int mpg123_close(void *mh);
IMPORT int mpg123_decode(void *mh, const unsigned char *inmemory, size_t inmemsize,	unsigned char *outmemory, size_t outmemsize, size_t *done);

JNIEXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_mp3_Mp3DecoderLibrary_create(JNIEnv *jni, jobject me) {
	mpg123_init();
	
	void* handle = mpg123_new(NULL, NULL);
	if (handle == NULL) {
		return 0;
	}
	
	if (mpg123_open_feed(handle) != 0) {
		mpg123_delete(handle);
		return 0;
	}
	
	return (jlong) handle;
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_mp3_Mp3DecoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	void* handle = (void*) instance;
	
	if (handle != NULL) {
		mpg123_close(handle);
		mpg123_delete(handle);
	}
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_mp3_Mp3DecoderLibrary_decode(JNIEnv *jni, jobject me, jlong instance, jobject direct_input,
		jint input_length, jobject direct_output, jint output_length) {
	
	if (instance == 0) {
		return -1;
	}
	
	unsigned char* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
	unsigned char* output = (*jni)->GetDirectBufferAddress(jni, direct_output);
	size_t used_bytes = 0;
	
	int result = mpg123_decode((void*) instance, input, (size_t) input_length, output, (size_t) output_length, &used_bytes);
	
	if (result != 0) {
		if (result > 0) {
			return -(result + 100);
		} else {
			return result;
		}
	}
	
	return (jint) used_bytes;
}
