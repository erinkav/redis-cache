#!/usr/bin/env bash
docker-compose build
docker-compose run sbt compile
docker-compose up -d