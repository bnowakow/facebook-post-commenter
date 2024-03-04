#!/bin/bash

# TODO for some reason "couldn.* th user on invite to fan page screen" doesn't work
docker compose logs -f | grep -P 'already posted this. Posting the same content repeatedly on Facebook isn|added comment to .* comments via|in .* post \[|fail to add comment to .* post|couldn.* th user on invite to fan page screen'

