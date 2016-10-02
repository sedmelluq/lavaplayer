cmake_minimum_required(VERSION 3.0)
project(connector C)

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
	add_definitions(-Dinline=__inline)
	include_directories("$ENV{JAVA_HOME}/include/win32")
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
	include_directories("$ENV{JAVA_HOME}/include/linux")
else()
	include_directories("$ENV{JAVA_HOME}/include/darwin")
endif()

include_directories("$ENV{JAVA_HOME}/include/")

include_directories("${samplerate_SOURCE_DIR}/src")
include_directories("../opus/opus-1.1.3/include")
include_directories("../fdk-aac/libAACdec/include")
include_directories("../fdk-aac/libSYS/include")
include_directories("../vorbis/libvorbis-1.3.5/include")
include_directories("../vorbis/libogg-1.3.2/include")
link_directories("../libs/${BITZ}")

if (DEFINED ENV{DIST_DIR})
	set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "$ENV{DIST_DIR}")
	set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE "$ENV{DIST_DIR}")
endif()

add_library(connector SHARED samplerate.c fdk-aac.c vorbis.c opus.c)
target_link_libraries(connector samplerate fdk-aac vorbis opus silk_common silk_fixed silk_float celt)
