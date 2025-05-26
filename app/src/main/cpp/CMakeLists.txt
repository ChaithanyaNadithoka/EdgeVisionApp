cmake_minimum_required(VERSION 3.10.2)

# Set project name
project("edgevision")

# Path to OpenCV
set(OpenCV_DIR "C:/Users/hp/Android/OpenCV-android-sdk/sdk/native/jni")

# Find OpenCV
find_package(OpenCV REQUIRED)

# Include OpenCV headers
include_directories(${OpenCV_INCLUDE_DIRS})

# Define native library
add_library(
        native-lib
        SHARED
        native-lib.cpp
        edge_detection.cpp
)

# Link OpenCV and Android log
target_link_libraries(
        native-lib
        ${OpenCV_LIBS}
        log
)
