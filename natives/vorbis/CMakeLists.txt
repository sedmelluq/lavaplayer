cmake_minimum_required(VERSION 3.0)

project(vorbis C)

get_filename_component(ROOT_DIR "." ABSOLUTE)
set(VORBIS_SOURCE "${ROOT_DIR}/libvorbis-1.3.5/lib")
set(OGG_SOURCE "${ROOT_DIR}/libogg-1.3.3/src")

if ("${CMAKE_C_COMPILER_ID}" STREQUAL "MSVC")
	set(CMAKE_C_FLAGS_RELEASE "/MT")
else()
	set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -fPIC")
endif()

file(GLOB MAIN_SOURCES "${VORBIS_SOURCE}/*.c" "${OGG_SOURCE}/*.c")

list(REMOVE_ITEM MAIN_SOURCES "${VORBIS_SOURCE}/psytune.c")
list(REMOVE_ITEM MAIN_SOURCES "${VORBIS_SOURCE}/tone.c")

include_directories("${ROOT_DIR}/libogg-1.3.3/include")
include_directories("${ROOT_DIR}/libvorbis-1.3.5/include")
include_directories("${ROOT_DIR}/libvorbis-1.3.5/lib")

add_library(vorbis STATIC ${MAIN_SOURCES})
