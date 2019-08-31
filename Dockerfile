# Import the base gradle image
FROM gradle:5.6.1-jdk8
MAINTAINER mrkirby153 <mrkirby153@mrkirby153.com>

# Add the files to the container
ADD . /build
# Perform the build
WORKDIR /build

RUN gradle build --no-daemon
RUN mkdir /kirbot
RUN find /build/build/distributions -type f -name 'KirBot-?.*\.tar' -exec tar --strip-components 1 -C /kirbot -xvf  '{}' ';'

WORKDIR /kirbot/bin

CMD ["./KirBot"]