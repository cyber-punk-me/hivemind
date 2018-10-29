# The Hivemind server implements API for machine learning driven apps and devices.

A very common pattern for ML driven apps and devices could be offloading heavy computations to servers while using pre-trained models on devices, which Tensorflow allows you to do. Hivemind helps to collect data and use it for ML in Tensorflow. After model was trained, you can either use it online or offline.

Hivemind is written in Kotlin with minimalistic dependencies, allowing you to use the same language for server and client development.

## STATUS alpha; implementing swagger schema : https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1

## Running
1. install docker https://www.docker.com/
    * optionally install nvidia docker runtime https://github.com/NVIDIA/nvidia-docker
    * recommended to `docker pull` : 
       * tensorflow/serving:latest
       * tensorflow/tensorflow:latest-gpu-py3 `OR` tensorflow/tensorflow:latest-py3
       * if you don't pull these images manually, they will have to be pulled before learning automatically, which is rather slow a.t.m.
1. docker client must be set up to work without `sudo` https://docs.docker.com/install/linux/linux-postinstall/
1. see examples folder for API referebce.
1. execute `./gradlew run` to start

## License

[Apache License 2.0](LICENSE)
