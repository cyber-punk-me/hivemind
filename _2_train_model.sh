#!/bin/bash
sudo nvidia-docker run -v `pwd`/local/model/myo-1:/myo-model --name myo -p 8888:8888 myo
