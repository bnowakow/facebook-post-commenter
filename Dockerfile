# https://stackoverflow.com/a/50467205
FROM gradle:jdk21 AS builder

ENV APP_HOME=/app
WORKDIR $APP_HOME
COPY build.gradle.kts gradle.properties gradlew $APP_HOME
COPY gradle $APP_HOME/gradle
RUN ./gradlew --console verbose --full-stacktrace shadowJar || return 0
COPY . .
RUN ./gradlew --console verbose --full-stacktrace shadowJar

# upgraded from bookworn to trixie since bookworm had only java-17 and trixie have java-21
FROM debian:trixie-20250428

ENV ARTIFACT_NAME=shadow-1.0-SNAPSHOT-all.jar
ENV APP_HOME=/app

WORKDIR $APP_HOME

# https://github.com/sunim2022/Jenkins_Docker/blob/9b55a490d3d83590a3eed3b064d73397a42d9de1/selenium-in-docker/Dockerfile
# Install tools.
RUN apt-get update -y  \
    && apt-get install -y wget unzip
ARG DEBIAN_FRONTEND=noninteractive
RUN apt-get install -y tzdata


# https://dev.to/sunim2022/run-your-selenium-tests-inside-docker-container-part-1-4b02
# Install Firefox
RUN echo "deb http://deb.debian.org/debian/ unstable main contrib non-free" >> /etc/apt/sources.list.d/debian.list
RUN apt-get update -qqy \
  && apt-get -qqy --no-install-recommends install firefox
#  && rm -rf /var/lib/apt/lists/* /var/cache/apt/*

RUN apt-get -qqy install firefox

# Install GeckoDriver
ARG GECKODRIVER_VERSION=0.36.0
RUN wget --no-verbose -O /tmp/geckodriver.tar.gz https://github.com/mozilla/geckodriver/releases/download/v$GECKODRIVER_VERSION/geckodriver-v$GECKODRIVER_VERSION-linux64.tar.gz \
  && rm -rf /opt/geckodriver \
  && tar -C /opt -zxf /tmp/geckodriver.tar.gz \
  && rm /tmp/geckodriver.tar.gz \
  && mv /opt/geckodriver /opt/geckodriver-$GECKODRIVER_VERSION \
  && chmod 755 /opt/geckodriver-$GECKODRIVER_VERSION \
  && ln -fs /opt/geckodriver-$GECKODRIVER_VERSION /usr/bin/geckodriver

RUN apt-get install -y openjdk-21-jdk

COPY --from=builder $APP_HOME/build/libs/$ARTIFACT_NAME .
# TODO: JSONArgsRecommended: JSON arguments recommended for ENTRYPOINT to prevent unintended behavior related to OS signals
# doesn't parse last variable ENTRYPOINT ["java", "-jar", $ARTIFACT_NAME]
ENTRYPOINT java -jar $ARTIFACT_NAME

