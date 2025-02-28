#!/bin/bash

git pull

# for error "network bridge not found" do sudo ~/code/bash_configs/nas/change-iptables-bridge-docker-settings.sh 
# TODO check if build was successfull
# docker compose build
# above fails on nas so as workadound we're building below on laptop
# https://www.docker.com/blog/multi-arch-build-and-images-the-simple-way/
# docker buildx create --use
# docker buildx build --push --platform linux/amd64  --tag bnowakow/facebook-post-commenter:latest .

while true; do
    docker buildx build --platform linux/amd64  --tag bnowakow/facebook-post-commenter:latest .
    yes | docker system prune
    time timeout 10h docker compose up
    docker compose down
    date
    #sleep 3h
    sleep 8h # for developer mode
    #sleep 24h # for Received Facebook error response of type OAuthException: (#80001) There have been too many calls to this Page account. Wait a bit and try again. For more info, please refer to https://developers.facebook.com/docs/graph-api/overview/rate-limiting. (code 80001, subcode null) 'null - null'
done

