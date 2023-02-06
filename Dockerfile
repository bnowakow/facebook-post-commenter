# https://stackoverflow.com/a/50467205
FROM gradle:jdk17 AS BUILDER

ENV APP_HOME=/app
WORKDIR $APP_HOME
COPY build.gradle.kts gradle.properties gradlew $APP_HOME
COPY gradle $APP_HOME/gradle
RUN ./gradlew --build-cache --console verbose --full-stacktrace shadowJar || return 0
COPY . .
RUN ./gradlew --build-cache --console verbose --full-stacktrace shadowJar

FROM gradle:jdk17

ENV ARTIFACT_NAME=shadow-1.0-SNAPSHOT-all.jar
ENV APP_HOME=/app
WORKDIR $APP_HOME
COPY --from=BUILDER $APP_HOME/build/libs/$ARTIFACT_NAME .
ENTRYPOINT java -jar $ARTIFACT_NAME

