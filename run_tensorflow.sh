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

curl -d '{"instances": [1.0, 2.0, 5.0]}' -X POST http://localhost:8080/model/5d335160-bd2a-45e4-9199-8105a38941ad/apply

docker stop tf_docker
docker rm -f tf_docker