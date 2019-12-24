#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/model/modelApplyInput
data=\
"3.125,26.0,2.0,4.0,9.875,79.0,2.0,3.0,2.875,32.0,3.0,3.0,2.875,30.0,1.0,2.0,3.125,47.0,5.0,5.0,21.125,283.0,5.0,5.0,15.875,186.0,5.0,5.0,14.375,177.0,5.0,4.0"
curl -d '{"instances" : [['$data']] }' \
-X POST http://localhost:8080/apply/1d722019-c892-44bc-844b-eb5708d55987
#-X POST http://localhost:8501/v1/models/1d722019-c892-44bc-844b-eb5708d55987:predict
echo
