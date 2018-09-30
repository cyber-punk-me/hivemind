#!/bin/bash
sudo docker rm -f model-1-servable
sudo docker run -p 8501:8501 \
--mount type=bind,source=$(pwd)/local/model/model-1/data,target=/models/model-1 \
-e MODEL_NAME=model-1 -t --name model-1-servable tensorflow/serving &