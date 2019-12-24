#!/usr/bin/env bash
docker ps -a | awk '{ print $1,$2 }' | grep tensorflow/serving | awk '{print $1 }' | xargs -I {} docker rm -f {}
docker ps -a | awk '{ print $1,$2 }' | grep tensorflow/tensorflow | awk '{print $1 }' | xargs -I {} docker rm -f {}
docker ps -a | awk '{ print $1,$2 }' | grep kyr7/emg-nn | awk '{print $1 }' | xargs -I {} docker rm -f {}
echo
