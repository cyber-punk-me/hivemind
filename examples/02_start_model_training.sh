#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/model/modelPost
curl -X POST "http://localhost:8080/model/1d722019-c892-44bc-844b-eb5708d55987?gpu=true&dockerPull=true" \
 -H "accept: application/json" -H "Content-Type: application/json" \
 -d "{ \"scriptId\": \"7de76908-d4d9-4ce9-98de-118a4fb3b8f8\", \"dataId\": \"46ffdf66-1941-4a6a-928a-ec7c8d4ec24a\" }}"