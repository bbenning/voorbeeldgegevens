#!/bin/bash
curl  -XPUT 'http://boot2docker:9200/_snapshot/my_backup' -d '{ "type": "fs", "settings": { "location": "/var/lib/elasticsearch/backups", "compress": true } }'




