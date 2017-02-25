#include <jni.h>
#include <vorbis/codec.h>
#include <stdlib.h>
#include <stdbool.h>

typedef struct vorbis_state_s {
	vorbis_info info;
	vorbis_comment comment;
	vorbis_block block;
	vorbis_dsp_state dsp_state;
	bool initialised;
} vorbis_state_t;

static void build_ogg_packet(JNIEnv* jni, ogg_packet* packet, jobject direct_buffer, jint offset, jint length, jboolean is_beginning) {
	unsigned char* packet_base = (*jni)->GetDirectBufferAddress(jni, direct_buffer);
	
	packet->packet = &packet_base[offset];
	packet->bytes = length;
	packet->b_o_s = is_beginning ? 1 : 0;
	packet->e_o_s = 0;
	packet->granulepos = 0;
	packet->packetno = 0;
}

JNIEXPORT jlong JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_create(JNIEnv *jni, jobject me) {
	vorbis_state_t* state = calloc(1, sizeof(*state));

	if (state == NULL) {
		return NULL;
	}

	vorbis_info_init(&state->info);
	vorbis_comment_init(&state->comment);

	return (jlong) state;
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_processHeader(JNIEnv *jni, jobject me, jlong instance, jobject direct_buffer, jint offset, jint length, jboolean is_beginning) {
	vorbis_state_t* state = (vorbis_state_t*) instance;
	ogg_packet packet;

	build_ogg_packet(jni, &packet, direct_buffer, offset, length, is_beginning);
	return vorbis_synthesis_headerin(&state->info, &state->comment, &packet);
}

JNIEXPORT jboolean JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_initialise(JNIEnv *jni, jobject me, jlong instance) {
	vorbis_state_t* state = (vorbis_state_t*) instance;
	ogg_packet packet;

	if (vorbis_synthesis_init(&state->dsp_state, &state->info) != 0) {
		return JNI_FALSE;
	}

	vorbis_block_init(&state->dsp_state, &state->block);
	state->initialised = true;
	return JNI_TRUE;
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_getChannelCount(JNIEnv *jni, jobject me, jlong instance) {
	vorbis_state_t* state = (vorbis_state_t*) instance;
	return state->info.channels;
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_input(JNIEnv *jni, jobject me, jlong instance, jobject direct_buffer, jint offset, jint length) {
	vorbis_state_t* state = (vorbis_state_t*) instance;
	ogg_packet packet;

	build_ogg_packet(jni, &packet, direct_buffer, offset, length, JNI_FALSE);

	int error = vorbis_synthesis(&state->block, &packet);
	if (error != 0) {
		return error;
	}
	
	return vorbis_synthesis_blockin(&state->dsp_state, &state->block);
}

JNIEXPORT jint JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_output(JNIEnv *jni, jobject me, jlong instance, jobjectArray channels, jint length) {
	vorbis_state_t* state = (vorbis_state_t*) instance;
	float** buffers;

	size_t available = (size_t) vorbis_synthesis_pcmout(&state->dsp_state, &buffers);
	size_t buffer_length = (size_t) length;

	size_t chunk = available > buffer_length ? buffer_length : available;

	if (chunk > 0) {
		for (int i = 0; i < state->info.channels; i++) {
			jfloatArray channel = (*jni)->GetObjectArrayElement(jni, channels, i);

			if (channel != NULL) {
				(*jni)->SetFloatArrayRegion(jni, channel, 0, chunk, buffers[i]);
			}
		}

		if ((*jni)->ExceptionCheck(jni)) {
			(*jni)->ExceptionClear(jni);
			return -1;
		}

		vorbis_synthesis_read(&state->dsp_state, (int) chunk);
	}

	return chunk;
}

JNIEXPORT void JNICALL Java_com_sedmelluq_discord_lavaplayer_natives_vorbis_VorbisDecoderLibrary_destroy(JNIEnv *jni, jobject me, jlong instance) {
	vorbis_state_t* state = (vorbis_state_t*) instance;

	if (state->initialised) {
		vorbis_block_clear(&state->block);
		vorbis_dsp_clear(&state->dsp_state);
	}

	vorbis_comment_clear(&state->comment);
	vorbis_info_clear(&state->info);

	free(state);
}
