cmake_minimum_required(VERSION 3.22.1)
project(skb_audio_core LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Oboe (AAR) requires the shared STL. Use c++_shared for the whole target.
set(ANDROID_STL c++_shared)

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

target_compile_features(skb_audio_core PRIVATE cxx_std_20)

find_library(log-lib log)
target_link_libraries(skb_audio_core PRIVATE
    oboe::oboe
    ${log-lib}
)
