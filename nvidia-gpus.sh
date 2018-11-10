#!/usr/bin/env bash
docker run --runtime=nvidia --rm nvidia/cuda:9.0-base nvidia-smi -L
