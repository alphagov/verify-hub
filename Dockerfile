ARG registry_image_gradle=gradle:6.9.2-jdk11
ARG registry_image_jdk=openjdk:11.0.16-jre@sha256:762d8d035c3b1c98d30c5385f394f4d762302ba9ee8e0da8c93344c688d160b2

FROM ${registry_image_gradle} as base-image

USER gradle
ENV GRADLE_USER_HOME /home/gradle

WORKDIR /verify-hub
USER root 
RUN chown -R gradle /verify-hub
USER gradle
COPY --chown=gradle build.gradle build.gradle
COPY --chown=gradle settings.gradle settings.gradle
COPY --chown=gradle idea.gradle idea.gradle
COPY --chown=gradle inttest.gradle inttest.gradle
COPY --chown=gradle publish.gradle publish.gradle

RUN gradle downloadDependencies

COPY --chown=gradle hub/shared/ hub/shared/
COPY --chown=gradle hub-saml/ hub-saml/
COPY --chown=gradle hub-saml-test-utils/ hub-saml-test-utils/
RUN gradle --console=plain \
    :hub-saml:build \
    :hub-saml:test \
    :hub-saml-test-utils:build \
    :hub-saml-test-utils:test \
    :hub:shared:build \
    :hub:shared:test

FROM ${registry_image_gradle} as build-app
ARG hub_app

USER gradle
ENV GRADLE_USER_HOME /home/gradle

WORKDIR /verify-hub
USER root 
RUN chown -R gradle /verify-hub
USER gradle

# Copy artifacts from previous image
COPY --chown=gradle --from=base-image /home/gradle /home/gradle
COPY --chown=gradle --from=base-image /verify-hub/hub/shared/build.gradle hub/shared/build.gradle
COPY --chown=gradle --from=base-image /verify-hub/hub/shared/src hub/shared/src
COPY --chown=gradle --from=base-image /verify-hub/hub/shared/build hub/shared/build
COPY --chown=gradle --from=base-image /verify-hub/hub-saml/build.gradle hub-saml/build.gradle
COPY --chown=gradle --from=base-image /verify-hub/hub-saml/src hub-saml/src
COPY --chown=gradle --from=base-image /verify-hub/hub-saml/build hub-saml/build
COPY --chown=gradle --from=base-image /verify-hub/hub-saml-test-utils/build.gradle hub-saml-test-utils/build.gradle
COPY --chown=gradle --from=base-image /verify-hub/hub-saml-test-utils/src hub-saml-test-utils/src
COPY --chown=gradle --from=base-image /verify-hub/hub-saml-test-utils/build hub-saml-test-utils/build
COPY --chown=gradle --from=base-image /verify-hub/build.gradle build.gradle
COPY --chown=gradle --from=base-image /verify-hub/settings.gradle settings.gradle
COPY --chown=gradle --from=base-image /verify-hub/idea.gradle idea.gradle
COPY --chown=gradle --from=base-image /verify-hub/inttest.gradle inttest.gradle
COPY --chown=gradle --from=base-image /verify-hub/publish.gradle publish.gradle


COPY --chown=gradle hub/$hub_app/build.gradle hub/$hub_app/build.gradle
COPY --chown=gradle hub/$hub_app/src hub/$hub_app/src

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

COPY --chown=gradle $conf_dir/$hub_app.yml /tmp/$hub_app.yml
COPY --chown=gradle --from=build-app /verify-hub/hub/$hub_app/build/install/$hub_app .

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
