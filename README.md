# Hivemind turns your Tensorflow project into an API

Collects data, trains, serves and distributes your Tensorflow models via REST API.

## STATUS alpha; implementing swagger schema : https://app.swaggerhub.com/apis/kyr7/hivemind/0.0.1

## Running
1. tested on ubuntu 18. CPU training should also run on Windows and Mac OS. 
2. install docker https://www.docker.com/
    * install nvidia docker runtime https://github.com/NVIDIA/nvidia-docker
    * recommended to `docker pull` : 
       * tensorflow/serving:latest
       * kyr7/emg-nn:nvidia `OR` kyr7/emg-nn:cpu if you set up CPU training
       * if you don't pull these images manually, they will have to be pulled before learning automatically, which is rather slow a.t.m.
3. choose your profile from [list](https://github.com/cyber-punk-me/emg-nn/blob/master/runconf.yml)
4. if you want training model other than cpu-based then set system property `profile` or env variable `HIVEMIND_PROFILE` with desired one
5. docker client must be set up to work without `sudo` https://docs.docker.com/install/linux/linux-postinstall/
6. see [examples](/examples) folder for API reference.
7. execute `./gradlew run` to start

## License

[Apache License 2.0](LICENSE)
