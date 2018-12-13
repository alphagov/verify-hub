verify-hub
=======

[![Build Status](https://travis-ci.org/alphagov/verify-hub.svg?branch=master)](https://travis-ci.org/alphagov/verify-hub)

Verify-hub contains the source code for some of the core  components of the 
GOV.UK Verify Hub:
* [policy](hub/policy/README.md)
* [config](hub/config/README.md)
* [saml-proxy](hub/saml-proxy/README.md)
* [saml-soap-proxy](hub/saml-soap-proxy/README.md)
* [config](hub/config/README.md)

The frontend is located [here](https://github.com/alphagov/verify-frontend/).

An technical overview of the Hub is available [here](doc/overview.md).

## Architectural Descision Records and documentation

We record our architectural decisions in `doc/adr`. We use [adr-tools](https://github.com/npryce/adr-tools) to help manage these decisions.

**Answers to some questions can be found on the [hub wiki](https://github.com/alphagov/verify-hub/wiki)**

## Prerequisites
The following software is required (installation notes follow)

1. Java 8 JDK (Oracle)
2. Git
3. Gradle
4. Intellij Community (Ultimate is fine as well)
5. Node.js
6. Ruby, with rbenv

### Java 8 JDK (Oracle)

  * Mac:
```
Get Installable from: http://www.oracle.com/technetwork/java/javase/downloads/index.html
Once Installed Set JAVA_HOME Environment Variable (required by Gradle)
Example: In mac add line to .bash_profile:
export JAVA_HOME='/Library/Java/JavaVirtualMachines/{your jdk version}/Contents/Home/'
```

  * Linux:
```
Detailed installation instructions can be found on Stack Overflow:
http://askubuntu.com/questions/56104/how-can-i-install-sun-oracles-proprietary-java-jdk-6-7-8-or-jre
Set JAVA_HOME environment variable in .bash_profile or similar (required by Gradle):
export JAVA_HOME=/usr/lib/jvm/{your-jdk-version}
```

### Gradle
  Either install Gradle, or use gradle wrapper
  * **Option 1 - Install Gradle**
    (ensure that you have gradle 4.0)

      * Mac: using homebrew
```
brew install gradle
```
      * Linux:
```
Manual install: http://www.gradle.org/downloads
Extract from zip, add gradle /bin directory to PATH environment variable in .bash_profile or similar.
```

  * **Option 2 - Use Gradle Wrapper**
  The core hub repo contains an instance of Gradle Wrapper which can be used straight from here without further installation. To use, call the ./gradlew script from core hub's root directory

### Node.js
  * Linux (Ubuntu)
```
Due to an ubuntu package name conflict, node was renamed to nodejs in apt
sudo apt-get install nodejs
Create symlink to restore original command name
ln -s /usr/bin/nodejs /usr/bin/node
```

### Ruby

There are some build scripts within the hub project that use ruby,
plus you'll require Ruby in order to use verify-puppet (Puppet and ops
tools) and the verify-boxes (infrastructure as code: VMs, firewalls, VPNs
etc) repo.

To avoid polluting your system ruby installation and to allow
switching between different Ruby versions, you should install rbenv
and then use that to install Rubies.

#### On Mac

Install [homebrew](http://brew.sh/) if you haven't already, then `brew
install rbenv`

#### On Linux

You probably want to follow the instructions to install
[rbenv](https://github.com/sstephenson/rbenv) and
[ruby-build](https://github.com/sstephenson/ruby-build)

#### Then, on all machines

```
rbenv global 2.4.2
```

## Configuration

### Gradle

The hub build.gradle runs a parallel build. The number of parallel forks used when running integration tests can be configured by setting the environment variable `ORG_GRADLE_PROJECT_IDA_HUB_MAX_PARALLEL_FORKS` (defaults to number of cores if not set). For example:

`export ORG_GRADLE_PROJECT_IDA_HUB_MAX_PARALLEL_FORKS=4`

## Start Development
From root core hub directory:
(replace gradle with ./gradlew if using gradle wrapper)

Run the build
```
gradle build
```

Setup IntelliJ Project files
```
gradle idea
```

## Before Committing
Run pre-commit script (build, unit test, package)
```
./pre-commit.sh
```

## Running Locally
To spin up hub services to test on your local machine
```
./startup.sh
```

To spin up all services in the Verify federation, clone [ida-hub-acceptance-tests](https://github.com/alphagov/ida-hub-acceptance-tests) as a sibling of verify-hub
```
cd ../ida-hub-acceptance-tests
./hub-startup.sh
```

## Support and raising issues

If you think you have discovered a security issue in this code please email [disclosure@digital.cabinet-office.gov.uk](mailto:disclosure@digital.cabinet-office.gov.uk) with details.

For non-security related bugs and feature requests please [raise an issue](https://github.com/alphagov/verify-hub/issues/new) in the GitHub issue tracker.

## Code of Conduct
This project is developed under the [Alphagov Code of Conduct](https://github.com/alphagov/code-of-conduct)
