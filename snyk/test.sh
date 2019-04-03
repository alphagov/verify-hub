#!/usr/bin/env bash

# If a new configuration is added it will need adding to `snyk_configurations.sh`.
# This script is intended to be used by a CI server.

# Travis builds run on Trusty which requires a different definition for sub-project
[[ $TRAVIS = "true" ]] && sub_project="config" || sub_project=":hub:config"
exit_code=0

function print_banner() {
    echo "######################################################################"
    echo "### Testing dependencies for $1 gradle configuration"
    echo "######################################################################"
}

function test_configuration() {
    local config=$1;
    print_banner "$config"
    # The sub-project specified here is irrelevant. The configuration will still be tested regardless of if the
    # sub-project actually uses it.
    snyk test --gradle-sub-project="$sub_project" -- --configuration="$config"
    if [[ $? -gt 0 ]]; then
        exit_code=1
    fi
}

source snyk/configurations.sh

mv Gemfile tmp-Gemfile
mv Gemfile.lock tmp-Gemfile.lock
trap "{ mv tmp-Gemfile Gemfile ; mv tmp-Gemfile.lock Gemfile.lock; }" EXIT

for configuration in $CONFIGURATIONS; do
   test_configuration $configuration
done

exit $exit_code