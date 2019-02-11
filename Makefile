test:
	docker-compose build
	docker-compose run e2e
	docker-compose down

