FROM gradle:jdk17

RUN mkdir -p /app
COPY . /app
WORKDIR /app

RUN gradle shadowJar
ENTRYPOINT java -jar ./build/libs/shadow-1.0-SNAPSHOT-all.jar