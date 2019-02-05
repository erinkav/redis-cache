FROM hseeberger/scala-sbt

RUN mkdir -p /redis-cache

WORKDIR /redis-cache

COPY . /redis-cache

EXPOSE 8080

RUN sbt run