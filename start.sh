#!/usr/bin/env bash
docker-compose build
docker-compose run scala sbt compile
docker-compose up
docker-compose run scala sbt test
docker-compose run scala sbt cucumber
docker-compose run scala sbt gatling:test