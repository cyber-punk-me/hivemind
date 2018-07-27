The Hivemind server implements API for machine learning driven apps and devices.
The cornerstone of the client-server ML duo is the computation engine. We select Tensorflow, since it has increasingly growing support across a range of hardware solutions.
A very common pattern for ML driven apps and devices could be offloading heavy computations to servers while using pre-trained models on devices, which tensorflow allows you to do.
Hivemind lets you train and test your models on a workstation and your own Android device, while giving flexible scalability with cloud ML providers support.
Hivemind is written in Kotlin with minimalistic dependencies, allowing you to use the same language for server and client development or implement a distributed data processing network.
