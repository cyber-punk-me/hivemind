# The Hivemind server implements API for machine learning driven apps and devices.

A very common pattern for ML driven apps and devices could be offloading heavy computations to servers while using pre-trained models on devices, which tensorflow allows you to do.

Hivemind is written in Kotlin with minimalistic dependencies, allowing you to use the same language for server and client development or implement a distributed data processing network.

## STATUS : implementing swagger schema : https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1

## Prototype instructions
1. place a Tensorflow script that can export a servable to 'pwd'/local/script/script-1 https://www.tensorflow.org/serving/serving_basic
⋅⋅* model training script should be 'pwd'/local/script/script-1/train.py
⋅⋅* model export base path is /script-1/data as currently assumed for the docker training container
2. run `'pwd'/_88_rebuild_model.sh` to build a docker image from your script and run training&servable export
⋅⋅* exported model will be coped to 'pwd'/models/model-1
3. run `'pwd'/_3_run_servable.sh` to start tf servable in docker
