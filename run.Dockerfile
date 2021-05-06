FROM  openjdk:11.0.9.1-jre

ARG config_location
ARG app_name

WORKDIR /app

COPY $config_location config.yml
COPY hub/$app_name/build/distributions/$app_name-0.1.local.zip $app_name.zip

RUN unzip $app_name.zip

CMD ${APP_NAME}-0.1.local/bin/${APP_NAME} server config.yml
