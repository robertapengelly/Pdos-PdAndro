cmake_minimum_required(VERSION 3.18.1)

project("pdos_pdandro")

add_executable(
        libpdos.so

        pdos.c)

add_executable(
        libls.so

        ls.c)

add_executable(libtest.so
        IMPORTED
        )

