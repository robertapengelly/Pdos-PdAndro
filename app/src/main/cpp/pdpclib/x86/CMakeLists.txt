cmake_minimum_required(VERSION 3.18.1)

project("pdos_pdandro")
enable_language(C ASM)

add_library(
	pdpclib
	STATIC
	
	linsupa.asm
	stdio.c
	string.c
	stdlib.c
	start.c
	time.c
	errno.c
	assert.c
	signal.c
	locale.c
	ctype.c
	setjmp.c
	math.c
	__memmgr.c
)

target_compile_options(pdpclib PRIVATE
    $<$<COMPILE_LANGUAGE:C>:-ffreestanding -fno-builtin -fno-stack-protector -nostdinc -nostdlib -D__gnu_linux__ -O0 -fno-builtin -DUSE_MEMMGR -I${CMAKE_CURRENT_SOURCE_DIR}>
    $<$<COMPILE_LANGUAGE:ASM>:-x assembler-with-cpp>
)

