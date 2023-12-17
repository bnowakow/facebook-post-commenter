#!/bin/bash

rm *.png
for file in $(docker exec facebook-post-commenter-facebook-post-commenter-1 find /app -name '*.png'); do 
    echo $file; 
    docker cp facebook-post-commenter-facebook-post-commenter-1:"$file" .
done

