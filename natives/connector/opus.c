#include "connector.h"
#include <opus.h>

CONNECTOR_EXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusEncoderLibrary_create(JNIEnv *jni, jobject me, jint sample_rate, jint channels, jint application, jint quality) {
	int error;
	OpusEncoder* encoder = opus_encoder_create(sample_rate, channels, application, &error);
	
	if (encoder != NULL) {
		opus_encoder_ctl(encoder, OPUS_SET_COMPLEXITY_REQUEST, quality);
	}
	
	return (jlong) encoder;
}

CONNECTOR_EXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusEncoderLibrary_encode(JNIEnv *jni, jobject me, jlong instance, jobject direct_input, jint frame_size,
		jobject direct_output, jint output_length) {
	if (instance == 0) {
		return 0;
	}

	opus_int16* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
	unsigned char* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

	return opus_encode((OpusEncoder*) instance, input, frame_size, output, output_length);
}

CONNECTOR_EXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusEncoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	OpusEncoder* encoder = (OpusEncoder*) instance;

	if (encoder != NULL) {
		opus_encoder_destroy(encoder);
	}
}

CONNECTOR_EXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusDecoderLibrary_create(JNIEnv *jni, jobject me, jint sample_rate, jint channels) {
	int error;
	return (jlong) opus_decoder_create(sample_rate, channels, &error);
}

CONNECTOR_EXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusDecoderLibrary_decode(JNIEnv *jni, jobject me, jlong instance, jobject direct_input, jint input_size,
	jobject direct_output, jint frame_size) {

	if (instance == 0) {
		return 0;
	}

	unsigned char* input = (*jni)->GetDirectBufferAddress(jni, direct_input);
	opus_int16* output = (*jni)->GetDirectBufferAddress(jni, direct_output);

	return opus_decode((OpusDecoder*) instance, input, input_size, output, frame_size, 0);
}

CONNECTOR_EXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_opus_OpusDecoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	OpusDecoder* decoder = (OpusDecoder*) instance;

	if (decoder != NULL) {
		opus_decoder_destroy(decoder);
	}
}
