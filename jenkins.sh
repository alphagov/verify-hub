#!/usr/bin/env bash
set -e # abort if you encounter an error
set -o pipefail # abort if there is an error in a piped operation
bin/build_and_upload_debs.rb
./gradlew -Pversion=$BUILD_NUMBER clean test intTest installDist publish bintrayUpload
./gradlew outputDependencies -q > dependencies.properties
