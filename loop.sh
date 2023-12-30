#!/bin/bash

git pull
# for error "network bridge not found" do sudo ~/code/bash_configs/nas/change-iptables-bridge-docker-settings.sh 
# TODO check if build was successfull
docker compose build

while true; do
    timeout 3600 docker compose up
    docker compose down
    date
    sleep 6h
done

