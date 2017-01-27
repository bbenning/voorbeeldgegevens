#!/bin/bash
PROJECT_DIR=`pwd`

docker ps -aq |xargs docker rm -f 
docker run -d -p 9200:9200 -p 9300:9300 -p 5601:5601 -p 5000:5000 -p 5044:5044 \
  -v $PROJECT_DIR/src/main/resources/data/backups:/var/lib/elasticsearch/backups-original \
  -v $PROJECT_DIR/src/main/resources/data/elasticsearch.yml:/etc/elasticsearch/elasticsearch.yml \
  --name techno-elk \
  sebp/elk:502

ELK_DOCKER_HASH=`docker ps |grep elk | awk '{print $1}'`
docker exec -ti $ELK_DOCKER_HASH cp -R /var/lib/elasticsearch/backups-original /var/lib/elasticsearch/backups
docker exec -ti $ELK_DOCKER_HASH chown -R elasticsearch:elasticsearch /var/lib/elasticsearch/backups
docker exec -ti $ELK_DOCKER_HASH /opt/kibana/bin/kibana-plugin install https://github.com/prelert/kibana-swimlane-vis/releases/download/v5.0.2/prelert_swimlane_vis-5.0.2.zip
