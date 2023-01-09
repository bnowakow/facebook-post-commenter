#!/bin/bash

docker-compose build

while true; do
    docker-compose up
    docker-compose down
    date
    sleep 60
done

