#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/model/modelPost
#not implemented
curl -X POST "http://localhost:8080/model/1d722019-c892-44bc-844b-eb5708d55986" \
 -H "accept: application/json" -H "Content-Type: application/json" \
 -d "{ \"scriptId\": \"1d722019-c892-44bc-844b-eb5708d57777\", \"dataId\": \"1d722019-c892-44bc-844b-eb5708d58888\" }}"