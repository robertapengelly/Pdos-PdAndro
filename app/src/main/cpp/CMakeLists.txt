cmake_minimum_required(VERSION 3.18.1)

project("pdos_pdandro")

if (${ANDROID_ABI} STREQUAL "armeabi-v7a" OR ${ANDROID_ABI} STREQUAL "x86")
#if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
add_subdirectory(${CMAKE_SOURCE_DIR}/pdpclib/${ANDROID_ABI})
add_subdirectory(${CMAKE_SOURCE_DIR}/bios/${ANDROID_ABI})
#add_subdirectory(${CMAKE_SOURCE_DIR}/pcomm/${ANDROID_ABI})
endif()

add_executable(
        libpdos.so

        pdos.c)

add_executable(
        libls.so

        ls.c)

add_executable(libtest.so
        IMPORTED
        )

