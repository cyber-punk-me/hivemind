#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/script/scriptPost
curl -X POST "http://localhost:8080/script/7de76908-d4d9-4ce9-98de-118a4fb3b8f8" \
 -H "accept: application/json" -H "Content-Type: multipart/form-data" -F "upfile=@myo-armband-nn-master.zip;type=application/zip"