#!/usr/bin/env bash

set -e
set +x
./gradlew checkJceInstalled || exit 1

function funky_pass_banner {
    tput setaf 2
    printf "\n########     ###     ######   ######  ######## ######## \n"
    printf "##     ##   ## ##   ##    ## ##    ## ##       ##     ##\n"
    printf "##     ##  ##   ##  ##       ##       ##       ##     ##\n"
    printf "########  ##     ##  ######   ######  ######   ##     ##\n"
    printf "##        #########       ##       ## ##       ##     ##\n"
    printf "##        ##     ## ##    ## ##    ## ##       ##     ##\n"
    printf "##        ##     ##  ######   ######  ######## ######## \n\n"
    tput sgr0
}

function funky_fail_banner {
    tput setaf 1
    printf "\n########    ###    #### ##       ######## ######## \n"
    printf "##         ## ##    ##  ##       ##       ##     ##\n"
    printf "##        ##   ##   ##  ##       ##       ##     ##\n"
    printf "######   ##     ##  ##  ##       ######   ##     ##\n"
    printf "##       #########  ##  ##       ##       ##     ##\n"
    printf "##       ##     ##  ##  ##       ##       ##     ##\n"
    printf "##       ##     ## #### ######## ######## ######## \n\n"
    tput sgr0
}

echo "Running tests"
./shutdown.sh

if ./gradlew --parallel --daemon clean build intTest copyToLib 2>/dev/null; then
  echo "Checking for dependency updates:"
  tput setaf 3
  ./gradlew -q dependencyUpdates -Drevision=release \
    | sed -n 's/uk.gov.ida:\(.*\) \[\(.*\)\]/\1 \2/p' 
  tput sgr0

  echo "Running services"
  if ./startup.sh skip-build; then
    funky_pass_banner
  fi
else
  funky_fail_banner

  echo "Test reports:"
  tput setaf 3
  find "$PWD" -name "index.html" | awk '{print "file://"$0}'
  tput sgr0
fi

./shutdown.sh
