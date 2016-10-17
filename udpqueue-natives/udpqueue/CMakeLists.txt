cmake_minimum_required(VERSION 3.0)
project(udpqueue C)

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
	include_directories("$ENV{JAVA_HOME}/include/win32")
	set(SYSNAME "win")
elseif(${CMAKE_SYSTEM_NAME} STREQUAL "Linux")
	include_directories("$ENV{JAVA_HOME}/include/linux")
	set(SYSNAME "linux")
else()
	include_directories("$ENV{JAVA_HOME}/include/darwin")
	set(SYSNAME "darwin")
endif()

if ("${CMAKE_C_COMPILER_ID}" STREQUAL "MSVC")
	add_definitions(-Dinline=__inline)
	set(CMAKE_C_FLAGS_RELEASE "/MT")
endif()

include_directories("$ENV{JAVA_HOME}/include/")

if (DEFINED ENV{DIST_DIR})
	set(CMAKE_LIBRARY_OUTPUT_DIRECTORY "$ENV{DIST_DIR}")
	set(CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE "$ENV{DIST_DIR}")
endif()

add_library(udpqueue SHARED udpqueue.c hashmap.c hashmap.h linked.c linked.h timing.h mutex.h ${SYSNAME}/timing.c ${SYSNAME}/mutex.c)

if(${CMAKE_SYSTEM_NAME} STREQUAL "Windows")
	target_link_libraries(udpqueue ws2_32)
endif()
