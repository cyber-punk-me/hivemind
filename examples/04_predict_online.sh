#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/model/modelApplyInput
#not implemented
curl -X POST "http://localhost:8080/apply/50842581-5b5c-4652-b6d1-35539ed471bb" \
 -H "accept: application/json" -H "Content-Type: application/form-data" -d "{ \"instances\": [ 1, 2, 3, 4 ]}"