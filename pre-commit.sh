#!/usr/bin/env bash

set -e
set +x

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

if [[ ! $(git secrets 2>/dev/null) ]]; then
  echo "⚠️ This repository should be checked against leaked AWS credentials ⚠️"
  echo "We highly recommend you run the following:"
  echo "   brew install git-secrets"
  echo "then to set up the git-secrets to run on each commit:"
  echo "   git secrets --install"
  echo "   git secrets --register-aws"
  echo " === !!! !!! !!! === "
  funky_fail_banner
  exit 1
else
  for hook in .git/hooks/commit-msg .git/hooks/pre-commit .git/hooks/prepare-commit-msg; do
    if ! grep -q "git secrets" $hook; then
      git secrets --install -f
    fi
  done
  git secrets --register-aws
fi

./gradlew checkJceInstalled || exit 1

echo "Running tests"
./shutdown.sh

if ./gradlew --parallel --daemon clean build intTest installDist 2>/dev/null; then
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
