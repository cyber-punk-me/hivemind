FROM tensorflow/tensorflow:latest-gpu-py3
RUN apt-get update
RUN apt-get install -y python3-pip
RUN apt-get install -y git
RUN python3 -m pip install numpy sklearn myo-python
RUN git clone --depth=1 https://github.com/kyr7/myo-armband-nn
VOLUME /myo-model
CMD cd myo-armband-nn && python3 train.py && cp -r data /myo-model