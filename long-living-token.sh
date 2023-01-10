#!/bin/bash

# https://developers.facebook.com/docs/facebook-login/guides/access-tokens/get-long-lived/

app_id=$(grep appId ./src/main/resources/facebook4j.properties | sed 's/.*=//')
app_secret=$(grep appSecret ./src/main/resources/facebook4j.properties | sed 's/.*=//')
access_token=$(grep accessToken ./src/main/resources/facebook4j.properties | sed 's/.*=//')

curl -i -X GET "https://graph.facebook.com/v15.0/oauth/access_token?grant_type=fb_exchange_token&client_id=$app_id&client_secret=$app_secret&fb_exchange_token=$access_token"

