cmake_minimum_required(VERSION 3.22.1)
project(skb_audio_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Oboe via Prefab (AAR)
find_package(oboe REQUIRED CONFIG)

add_library(skb_audio_core SHARED
    native-lib.cpp
    skb_audio_core/WavReader.cpp
    skb_audio_core/AudioPlayer.cpp
)

target_include_directories(skb_audio_core PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_options(skb_audio_core PRIVATE
    -Wall -Wextra -Wno-unused-parameter -O2
)

find_library(log-lib log)
target_link_libraries(skb_audio_core PRIVATE
    oboe::oboe
    ${log-lib}
)
