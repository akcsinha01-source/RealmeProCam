cmake_minimum_required(VERSION 3.22.1)

project("procam_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Optimization flags for Snapdragon 7s Gen 3 (ARM64-v8a)
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -ffast-math -Wall")

add_library(
    procam_native
    SHARED
    focus_stacker.cpp
    image_proc_native.cpp
)

find_library(
    log-lib
    log
)

find_library(
    jnigraphics-lib
    jnigraphics
)

target_link_libraries(
    procam_native
    ${log-lib}
    ${jnigraphics-lib}
    m
)
