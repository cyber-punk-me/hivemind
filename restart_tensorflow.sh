#!/bin/bash
# http://localhost:8888/?token=8d7bbb762478fa8de5418d45e3665b39c95449cd9468acfc
# restart
docker restart tensorflow
# ssh
docker exec -i -t tensorflow bash
docker logs tensorflow
docker exec -it tensorflow jupyter notebook list