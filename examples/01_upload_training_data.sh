#!/bin/bash
#https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1#/data/dataPost
#generate a uuid and post. each post will create a new file named 0,1,..N
#you can use data from myo-armband-nn for testing
data="1.0, 2.0, 5.0"
curl -d "$data" -X POST http://localhost:8080/data/5d335160-bd2a-45e4-9199-8105a38941ad?ext=csv