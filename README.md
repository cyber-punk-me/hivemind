# The Hivemind server implements API for machine learning driven apps and devices.

A very common pattern for ML driven apps and devices could be offloading heavy computations to servers while using pre-trained models on devices, which Tensorflow allows you to do. Hivemind helps to collect data and use it for ML in Tensorflow. After model was trained, you can either use it online or offline.

Hivemind is written in Kotlin with minimalistic dependencies, allowing you to use the same language for server and client development.

## STATUS pre-alpha; implementing swagger schema : https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1

## Prototype instructions
1. install docker https://www.docker.com/ 
2. docker client should work without sudo https://docs.docker.com/install/linux/linux-postinstall/
3. see examples folder for API referebce.
4. execute ./gradlew run

check training status with `docker ps`  for now.

## License

[Apache License 2.0](LICENSE)
