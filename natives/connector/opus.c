#include <jni.h>
#include <opus.h>

JNIEXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusEncoderLibrary_create(JNIEnv *jni, jobject me, jint sample_rate, jint channels, jint application, jint quality) {
	int error;
	OpusEncoder* encoder = opus_encoder_create(sample_rate, channels, application, &error);
	
	if (encoder != NULL) {
		opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY_REQUEST, quality);
	}
	
	return (jlong) encoder;
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusEncoderLibrary_encode(JNIEnv *jni, jobject me, jlong instance, jobject direct_input, jint frame_size,
		jobject direct_output, jint output_length) {
	if (instance == 0) {
		return 0;
	}

	opus_int16* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
	unsigned char* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

	return opus_encode((OpusEncoder*) instance, input, frame_size, output, output_length);
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusEncoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	if (instance != 0) {
		opus_encoder_destroy(instance);
	}
}

JNIEXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusDecoderLibrary_create(JNIEnv *jni, jobject me, jint sample_rate, jint channels) {
	int error;
	return (jlong) opus_decoder_create(sample_rate, channels, &error);
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusDecoderLibrary_decode(JNIEnv *jni, jobject me, jlong instance, jobject direct_input, jint input_size,
	jobject direct_output, jint frame_size) {

	if (instance == 0) {
		return 0;
	}

	unsigned char* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
	opus_int16* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

	return opus_decode((OpusDecoder*) instance, input, input_size, output, frame_size, 0);
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusDecoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	if (instance != 0) {
		opus_decoder_destroy(instance);
	}
}
