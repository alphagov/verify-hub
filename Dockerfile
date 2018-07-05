FROM gradle:4.7.0-jdk8 as build
ARG hub_app

WORKDIR /$hub_app
USER root
ENV GRADLE_USER_HOME ~/.gradle

COPY build.gradle build.gradle
COPY settings.gradle settings.gradle
COPY shared.gradle shared.gradle
COPY idea.gradle idea.gradle
COPY inttest.gradle inttest.gradle

COPY hub/$hub_app/build.gradle /$hub_app/hub/$hub_app/build.gradle
COPY hub-saml/build.gradle hub-saml/build.gradle

# There is an issue running idea.gradle in the container
# So just make this an empty file
RUN gradle :hub:$hub_app:install -x jar

COPY hub/$hub_app/src hub/$hub_app/src
COPY hub-saml/src hub-saml/src

RUN gradle --no-daemon :hub:$hub_app:installDist

ENTRYPOINT ["gradle", "--no-daemon"]
CMD ["tasks"]

FROM openjdk:8-jre
ARG hub_app

WORKDIR /$hub_app

COPY configuration/local/$hub_app.yml $hub_app.yml
COPY --from=build /$hub_app/hub/$hub_app/build/install/$hub_app .

# ARG is not available at runtime so set an env var with
# name of app/app-config to run
ENV HUB_APP $hub_app
CMD bin/$HUB_APP server $HUB_APP.yml
