#!/bin/bash
sudo nvidia-docker run -v `pwd`/local/model/model-1:/model-1 \
--name model-1 -p 8888:8888 model-1
