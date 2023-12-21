#!/bin/bash

wc -l src/main/resources/adPosts.txt
mv src/main/resources/adPosts.txt src/main/resources/adPosts.txt-to-be-processed
cat src/main/resources/adPosts.txt-to-be-processed | sort | uniq  > src/main/resources/adPosts.txt
rm src/main/resources/adPosts.txt-to-be-processed
wc -l src/main/resources/adPosts.txt

