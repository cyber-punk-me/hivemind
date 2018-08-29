#!/bin/bash
cd local/script/script-1
sudo docker build -f ../../../docker-train/Dockerfile -t "model-1" .