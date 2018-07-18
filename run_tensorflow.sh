#!/bin/bash
docker run --name tensorflow -p 8888:8888 -v /opt/tf_data:/data tensorflow/tensorflow

