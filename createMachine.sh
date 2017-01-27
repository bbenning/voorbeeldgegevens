#!/bin/bash

docker-machine create -d virtualbox --virtualbox-disk-size "80000" --virtualbox-memory "8096" --virtualbox-cpu-count "4" giant-dev

eval $(docker-machine env giant-dev)

docker-machine ssh giant-dev "sudo sh -c 'echo "sudo sysctl -w vm.max_map_count=262144" >> /var/lib/boot2docker/profile'"

