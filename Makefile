test:
	chmod +x run_tests.sh
	chmod +x start.sh
	./start.sh
	./test.sh
build-sbt-docker-image:
	docker build --tag sbt_wrapper --file sbt_wrapper.Dockerfile .
api-test:
