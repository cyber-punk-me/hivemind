#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/data/dataPost
#generate a uuid and post. each post will create a new file named 0,1,..N
#curl -d "data" -X POST http://localhost:8080/data/5d335160-bd2a-45e4-9199-8105a38941ad?ext=csv
rm -f data.zip
rm -r data

wget -O data.zip https://github.com/cyber-punk-me/test-emg-features/archive/master.zip
unzip data.zip -d data

for filename in data/test-emg-features-master/*.csv; do
    echo "uploading $filename"
    #example of posting data the way an app could do it
    curl --data-binary @- -X POST http://localhost:8080/data/5d335160-bd2a-45e4-9199-8105a38941ad?ext=.csv \
      -H "accept: application/json" <<< "$(cat $filename)"
done