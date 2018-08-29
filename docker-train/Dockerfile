FROM tensorflow/tensorflow:latest-gpu-py3
RUN apt-get update
RUN apt-get install -y python3-pip
RUN apt-get install -y git
RUN python3 -m pip install numpy sklearn myo-python
ADD . /script-1/
VOLUME /model-1
CMD cd /script-1 && ls && python3 train.py && cp -r data /model-1