#!/usr/bin/env bash
set -e # abort if you encounter an error
set -o pipefail # abort if there is an error in a piped operation
./gradlew -Pversion=$BUILD_NUMBER clean test intTest copyToLib publish
bin/build_and_upload_debs.rb
./gradlew outputDependencies -q > dependencies.properties
