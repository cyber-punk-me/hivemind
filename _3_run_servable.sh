#!/bin/bash
sudo docker run -p 8501:8500 \
--mount type=bind,source=$(pwd)/local/model/myo-1/data,target=/models/myo-1 \
-e MODEL_NAME=myo-1 -t tensorflow/serving &