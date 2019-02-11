FROM gradle:5.1.0-jdk11 as base-image
USER root
ENV GRADLE_USER_HOME /usr/gradle/.gradle

WORKDIR /verify-hub
COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY idea.gradle idea.gradle
COPY inttest.gradle inttest.gradle

RUN gradle downloadDependencies

COPY hub-saml/ hub-saml/
COPY hub-saml-test-utils/ hub-saml-test-utils/
RUN gradle --console=plain \
    :hub-saml:build \
    :hub-saml:test \
    :hub-saml-test-utils:build \
    :hub-saml-test-utils:test

FROM gradle:5.1.0-jdk11 as build-app
USER root
ENV GRADLE_USER_HOME /usr/gradle/.gradle

ARG hub_app
WORKDIR /verify-hub

# Copy artifacts from previous image
COPY --from=base-image /usr/gradle/.gradle /usr/gradle/.gradle
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

COPY hub/$hub_app/build.gradle hub/$hub_app/build.gradle
COPY hub/$hub_app/src hub/$hub_app/src

RUN gradle --console=plain \
    :hub:$hub_app:installDist \
    :hub:$hub_app:test \
    :hub:$hub_app:intTest \
    # Don't rebuild hub-saml or hub-saml-test-utils
    -x :hub-saml:jar \
    -x :hub-saml-test-utils:jar

FROM openjdk:11-jre
ARG hub_app

WORKDIR /verify-hub

COPY configuration/local/$hub_app.yml $hub_app.yml
COPY --from=build-app /verify-hub/hub/$hub_app/build/install/$hub_app .

# ARG is not available at runtime so set an env var with
# name of app/app-config to run
ENV HUB_APP $hub_app
ENTRYPOINT ["sh", "-c", "bin/$HUB_APP"]
CMD ["server", "$HUB_APP.yml"]
