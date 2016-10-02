cmake_minimum_required(VERSION 3.0)

project(vorbis C)

get_filename_component(ROOT_DIR "." ABSOLUTE)
set(VORBIS_SOURCE "${ROOT_DIR}/libvorbis-1.3.5/lib")
set(OGG_SOURCE "${ROOT_DIR}/libogg-1.3.2/src")

file(GLOB MAIN_SOURCES "${VORBIS_SOURCE}/*.c" "${OGG_SOURCE}/*.c")

list(REMOVE_ITEM MAIN_SOURCES "${VORBIS_SOURCE}/psytune.c")
list(REMOVE_ITEM MAIN_SOURCES "${VORBIS_SOURCE}/tone.c")

include_directories("${ROOT_DIR}/libogg-1.3.2/include")
include_directories("${ROOT_DIR}/libvorbis-1.3.5/include")

add_library(vorbis STATIC ${MAIN_SOURCES})
