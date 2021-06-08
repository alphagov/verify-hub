ARG registry_image_gradle=gradle:6.7.0-jdk11
ARG registry_image_jdk=openjdk:11.0.11-jre@sha256:e12869c3edad94f2c23a40d0fe21b307a1a9177a621c9b902ecfa02897ba93f7

FROM ${registry_image_gradle} as base-image

USER root
ENV GRADLE_USER_HOME /usr/gradle/.gradle

WORKDIR /verify-hub
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY idea.gradle idea.gradle
COPY inttest.gradle inttest.gradle
COPY publish.gradle publish.gradle

RUN gradle downloadDependencies

COPY hub/shared/ hub/shared/
COPY hub-saml/ hub-saml/
COPY hub-saml-test-utils/ hub-saml-test-utils/
RUN gradle --console=plain \
    :hub-saml:build \
    :hub-saml:test \
    :hub-saml-test-utils:build \
    :hub-saml-test-utils:test \
    :hub:shared:build \
    :hub:shared:test

FROM ${registry_image_gradle} as build-app
ARG hub_app
USER root
ENV GRADLE_USER_HOME /usr/gradle/.gradle

WORKDIR /verify-hub

# Copy artifacts from previous image
COPY --from=base-image /usr/gradle/.gradle /usr/gradle/.gradle
COPY --from=base-image /verify-hub/hub/shared/build.gradle hub/shared/build.gradle
COPY --from=base-image /verify-hub/hub/shared/src hub/shared/src
COPY --from=base-image /verify-hub/hub/shared/build hub/shared/build
COPY --from=base-image /verify-hub/hub-saml/build.gradle hub-saml/build.gradle
COPY --from=base-image /verify-hub/hub-saml/src hub-saml/src
COPY --from=base-image /verify-hub/hub-saml/build hub-saml/build
COPY --from=base-image /verify-hub/hub-saml-test-utils/build.gradle hub-saml-test-utils/build.gradle
COPY --from=base-image /verify-hub/hub-saml-test-utils/src hub-saml-test-utils/src
COPY --from=base-image /verify-hub/hub-saml-test-utils/build hub-saml-test-utils/build
COPY --from=base-image /verify-hub/build.gradle build.gradle
COPY --from=base-image /verify-hub/settings.gradle settings.gradle
COPY --from=base-image /verify-hub/idea.gradle idea.gradle
COPY --from=base-image /verify-hub/inttest.gradle inttest.gradle
COPY --from=base-image /verify-hub/publish.gradle publish.gradle


COPY hub/$hub_app/build.gradle hub/$hub_app/build.gradle
COPY hub/$hub_app/src hub/$hub_app/src

RUN gradle --console=plain \
    :hub:$hub_app:installDist \
    :hub:$hub_app:test \
    :hub:$hub_app:intTest \
    # Don't rebuild hub-saml or hub-saml-test-utils \
    -x :hub:shared:jar \
    -x :hub-saml:jar \
    -x :hub-saml-test-utils:jar

FROM ${registry_image_jdk}
ARG hub_app
ARG release=local-dev
ARG conf_dir=configuration

WORKDIR /verify-hub

COPY $conf_dir/$hub_app.yml /tmp/$hub_app.yml
COPY --from=build-app /verify-hub/hub/$hub_app/build/install/$hub_app .

# set a sensible default for java's DNS cache
# if left unset the default is to cache forever
RUN echo "networkaddress.cache.ttl=5" >> /usr/local/openjdk-11/conf/security/java.security

# ARG is not available at runtime so set an env var with:
# Name of app/app-config to run
ENV HUB_APP $hub_app
# Sentry release information
ENV SENTRY_RELEASE $release
ENV SENTRY_DIST x86

CMD bin/$HUB_APP server /tmp/$HUB_APP.yml
