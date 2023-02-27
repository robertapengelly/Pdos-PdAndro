cmake_minimum_required(VERSION 3.18.1)

project("pdos_pdandro")
enable_language(C)

add_executable(
	libbios.so
	
	${CMAKE_SOURCE_DIR}/pdpclib/${ANDROID_ABI}/linstart.c
	bios.c
	exeload.c
)

target_compile_options(libbios.so PRIVATE
    $<$<COMPILE_LANGUAGE:C>:-ffreestanding -fno-builtin -fno-stack-protector -nostdinc -nostdlib -O0 -D__UNOPT__ -D__ARM__ -DNEED_AOUT -DNEED_MPROTECT -I${CMAKE_SOURCE_DIR}/pdpclib/${ANDROID_ABI} -I${CMAKE_CURRENT_SOURCE_DIR}>
)

target_link_options(libbios.so PRIVATE "-Wl,-nostdlib")
target_link_options(libbios.so PRIVATE "-nostdlib")

target_link_libraries(libbios.so PRIVATE pdpclib)

