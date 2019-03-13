#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/model/modelGet
curl -X GET "http://localhost:8080/model/1d722019-c892-44bc-844b-eb5708d55987" -H "accept: attachment; filename=model.zip"