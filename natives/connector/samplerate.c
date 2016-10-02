#include <jni.h>
#include <samplerate.h>

JNIEXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_samplerate_SampleRateLibrary_create(JNIEnv *jni, jobject me, jint type, jint channels) {
	int error;
	return (jlong)src_new(type, channels, &error);
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_samplerate_SampleRateLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	src_delete((SRC_STATE*)instance);
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_samplerate_SampleRateLibrary_reset(JNIEnv *jni, jobject me, jlong instance) {
	src_reset((SRC_STATE*)instance);
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_samplerate_SampleRateLibrary_process(JNIEnv *jni, jobject me, jlong instance,
		jfloatArray in_array, jint in_offset, jint in_length, jfloatArray out_array, jint out_offset, jint out_length, jboolean end_of_input,
		jdouble source_ratio, jintArray progress_array) {

	float* in = (*jni)->GetPrimitiveArrayCritical(jni, in_array, NULL);
	float* out = (*jni)->GetPrimitiveArrayCritical(jni, out_array, NULL);

	SRC_DATA data;
	data.data_in = &in[in_offset];
	data.input_frames = in_length;
	data.input_frames_used = 0;
	data.end_of_input = end_of_input;
	data.data_out = &out[out_offset];
	data.output_frames = out_length;
	data.output_frames_gen = 0;
	data.src_ratio = source_ratio;

	int result = src_process((SRC_STATE*)instance, &data);

	(*jni)->ReleasePrimitiveArrayCritical(jni, in_array, in, JNI_ABORT);
	(*jni)->ReleasePrimitiveArrayCritical(jni, out_array, out, 0);

	int progress[2] = { data.input_frames_used, data.output_frames_gen };
	(*jni)->SetIntArrayRegion(jni, progress_array, 0, 2, progress);

	return result;
}