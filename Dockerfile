FROM gradle:jdk17

RUN apt-get update
RUN apt-get install firefox xvfb -y
RUN wget https://github.com/mozilla/geckodriver/releases/download/v0.32.0/geckodriver-v0.32.0-linux64.tar.gz
RUN tar zxf geckodriver*tar.gz
RUN mv geckodriver /usr/bin/geckodriver


RUN mkdir -p /app
COPY . /app
WORKDIR /app

RUN gradle shadowJar


#ENTRYPOINT java -jar ./build/libs/shadow-1.0-SNAPSHOT-all.jar
ENTRYPOINT DISPLAY=:1 xvfb-run java -jar ./build/libs/shadow-1.0-SNAPSHOT-all.jar

