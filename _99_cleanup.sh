#!/bin/bash
docker rmi -f model-1
docker rm -f model-1
docker rm -f $(docker ps -a --format "table {{.ID}}\t{{.Labels}}" | grep service=training | awk '{print $1}')
docker rm -f $(docker ps -a --format "table {{.ID}}\t{{.Labels}}" | grep service=serving | awk '{print $1}')
sudo rm -r local/model/*
sudo rm -r local/scipt/7de76908-d4d9-4ce9-98de-118a4fb3b8f8/data