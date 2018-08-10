#!/bin/bash
#https://www.tensorflow.org/serving/docker
#https://github.com/tensorflow/serving/blob/master/tensorflow_serving/g3doc/serving_advanced.md

mkdir -p /tmp/tfserving
cd /tmp/tfserving
git clone --depth=1 https://github.com/tensorflow/serving

docker run -d --name tf_docker -p 8501:8501 -v /tmp/tfserving/serving/tensorflow_serving/servables/tensorflow/testdata/saved_model_half_plus_three:/models/half_plus_three -e MODEL_NAME=half_plus_three -t tensorflow/serving tf_docker

#train a model(TODO)
docker exec -i -t tf_docker bazel-bin/tensorflow_serving/example/mnist_saved_model /tmp/mnist_model
docker exec -i -t tf_docker bazel build -c opt //tensorflow_serving/example:mnist_saved_model


#feed data(prebuilt model)
curl -d '{"instances": [1.0, 2.0, 5.0]}' -X POST http://localhost:8501/v1/models/half_plus_three:predict

#feed from docker(prebuilt model)
docker exec -i -t tf_docker curl -d '{"instances": [1.0, 2.0, 5.0]}' -X POST http://localhost:8501/v1/models/half_plus_three:predict

docker stop tf_docker
docker rm -f tf_docker

predict:
curl -d '{"instances": [1.0, 2.0, 5.0]}' -X POST http://localhost:8080/model/5d335160-bd2a-45e4-9199-8105a38941ad/apply
download data:
curl -X GET http://localhost:8080/data/5d335160-bd2a-45e4-9199-8105a38941ad --output deleteme
upload data:
curl -d '1.0, 2.0, 5.0' -X POST http://localhost:8080/data/5d335160-bd2a-45e4-9199-8105a38941ad

TF + docker ubuntu (18):
https://github.com/NVIDIA/nvidia-docker
#needs nvidia-persistenced
https://github.com/NVIDIA/nvidia-persistenced
#check : sudo nvidia-docker run --rm nvidia/cuda nvidia-smi
#tf-docker-gpu
sudo nvidia-docker run --rm nvidia/cuda nvidia-smi
sudo nvidia-docker run --rm --name tf1 -p 8888:8888 -p 6006:6006 redaboumahdi/image_processing:gpu jupyter notebook --allow-root