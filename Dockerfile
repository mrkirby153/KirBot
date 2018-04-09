# Import the base maven image
FROM maven:3.5-jdk-8-alpine
MAINTAINER mrkirby153 <mrkirby153@mrkirby153.com>

# Add the files to the container
ADD . /build
# Add MySQL Client for DB backups
RUN apk update && apk add --no-cache mysql-client
# Perform the build
RUN cd /build && \
    mvn clean install && \
    mkdir /kirbot && \
    find /build/target -type f -name 'KirBot-?.*\.jar' -exec cp '{}' /kirbot/KirBot.jar ';' && \
    rm -rf /build && \
    rm -rf ~/.m2

WORKDIR /kirbot

CMD ["java", "-jar", "KirBot.jar"]