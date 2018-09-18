#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/model/modelPost
#not implemented
curl -X POST "http://localhost:8080/model/1d722019-c892-44bc-844b-eb5708d55986" \
 -H "accept: application/json" -H "Content-Type: application/json" \
 -d "{ \"meta\": { \"id\": \"string\", \"name\": \"svm-1\", \"note\": \"string\", \"time\": \"2018-09-18T21:40:13.141Z\", \"tags\": [ \"string\" ] }, \"scriptId\": \"string\", \"dataId\": \"string\", \"runStatus\": { \"state\": \"new\", \"startTime\": \"2018-09-18T21:40:13.141Z\", \"endTime\": \"2018-09-18T21:40:13.141Z\", \"scriptId\": \"string\", \"modelId\": \"string\", \"dataId\": \"string\" }}"