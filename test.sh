#!/usr/bin/env bash

docker-compose build
docker-compose run scala sbt compile
docker-compose up
docker-compose scala sbt test
docker-compose scala sbt cucumber
docker-compose scala sbt:gatling