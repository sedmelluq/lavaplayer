cmake_minimum_required(VERSION 3.0)

project(fdk-aac CXX)

get_filename_component(ROOT_DIR "." ABSOLUTE)

set(MAIN_DIRS "${ROOT_DIR}/libAACdec" "${ROOT_DIR}/libFDK" "${ROOT_DIR}/libSYS" "${ROOT_DIR}/libMpegTPDec" "${ROOT_DIR}/libSBRdec" "${ROOT_DIR}/libPCMutils")
set(MAIN_SOURCES "")

message(STATUS "${MAIN_DIRS}")

foreach(subdir ${MAIN_DIRS})
	file (GLOB dir_sources "${subdir}/src/*.cpp" "${subdir}/src/*.h" "${subdir}/include/*.h")
	set (MAIN_SOURCES ${MAIN_SOURCES} ${dir_sources})
	
	include_directories("${subdir}/include")
endforeach()

if ("${CMAKE_CXX_COMPILER_ID}" STREQUAL "MSVC")
	add_definitions(-Dinline=__inline)
	set(CMAKE_CXX_FLAGS_RELEASE "/MT")
else()
	add_definitions(-Wno-narrowing)
	set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC")
endif()

add_library(fdk-aac STATIC ${MAIN_SOURCES})
