#!/usr/bin/env bash

cd $(dirname "${BASH_SOURCE[0]}")

app="$1"

./gradlew :hub:$app:clean
./gradlew :hub:$app:distZip -Pversion=local
docker build -t $app:latest --build-arg config_location=configuration/local/$app.yml --build-arg app_name=$app  -f run.Dockerfile .
echo "$app:latest"
