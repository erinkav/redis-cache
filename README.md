## Running the application

```
> git clone https://github.com/erinkav/redis-cache

> cd redis-cache

> make test
```

## Architecture

This redis proxy was implemented using Akka HTTP and the actor system to pass messages through the server. The two Actor classes are the RedisActor and the LocalLFUCacheActor.
*RedisActor*: interfaces with the Redis cache to check if values are present. The Redis cache is also responsible for telling the LocalLFUCacheActor when it finds a value so it can be stored.
*LocalLFUCacheActor*: has methods to get and to set values on the cache. Interfaces with the implementation of the LFU cache to get and set values in the local cache.

How requests is routed through the actor system:
```
  Route ->
     GET /key ->
         LocalLFUCacheActor ->
             LocalLFUCache ->
                key found: LocalLFUCacheActor sends value to its caller
                key not present:
                    LocalLFUCacheActor ->
                         RedisActor  ->
                             RedisActor sends value to caller
                             RedisActor ->
                                 LocalLFUCacheActor to save value in cache
     GET /cache/key ->
         routeCache ->
            - key found: value returned
            - key not present:
                    RedisActor ->
                         RedisActor sends value to caller
                         Akka caches value for route
```

LFU Cache:

The cache settings are included in the application.conf file. The cache will evict any entry that exceeds the expiration limit when get is called on the value.
The least frequently accessed value in the cache will be evicted when the cache hits it's max key size.

```
  localCache {
    expiry = 2 m
    fixedSize = 200
  }
```

## Testing

Unit tests can be run with `sbt test`

### End to End Tests

Basic end to end scenarios are set up to test the API. Run `sbt cucumber` to start the tests when the application is running

### Performance Tests
Gatling is set up to performance test the API. The settings are configurable in the test file. To run the tests, run `sbt gatling:test`
The performance tests are hitting both endpoints - one with the Akka LFU Cache implemented on the route and one with a separate implementation of the LFU cache

## Concurrency and blocking calls

The recommended Scala Redis client is  blocking. To mitigate that to increase performance you can implement a connection pool: https://github.com/debasishg/scala-redis#implementing-asynchronous-patterns-using-pooling-and-futures
This implementation relies on the asynchronous communication between actors. The number of concurrent connections to the server is configurable in the akka settings in the config.

## Features and Implementation

##### HTTP web service**
Clients interface to the Redis proxy through HTTP, with the Redis “GET” command mapped to the HTTP “GET” method. (1 hour)

##### Single backing instance
Each instance of the proxy service is associated with a single Redis service (5 minutes)

##### Cached GET
A GET request, directed at the proxy, returns the value of the specified key from the proxy’s local cache if the local cache contains a value for that key. If the local cache
does not contain a value for the specified key, it fetches the value from the backing Redis instance, using the Redis GET command, and stores it in the local cache,associated with the specified key. (1.5 hours)
 - Implemented first using the Akka lfu cache on the route. Later implemented in the project by creating a localLFUCache. The cache has at worst linear time get and set if all keys are viewed with the same frequency (45 min)
 - Global expiry - Entries added to the proxy cache are expired after being in the cache for a time (10 minutes)
 - LRU eviction -  Once the cache fills to capacity, the least recently used key is evicted (15 minutes)
 - Fixed key size The cache capacity is configured in terms of number of keys it retains. (5 minutes)

##### Concurrent processing
Multiple clients are able to concurrently connect to the proxy up to some configurable maximum limit (1.5 hour)
 - Implemented asynchronous communication between actors to enable concurrent requests.
 - Set up Gatling test suite to test how the API handles the load

##### Configuration
The following parameters are configurable at the proxy startup: Address of the backing Redis, Cache expiry time, Capacity, number of keys, TCP/IP port number the proxy listens on (10 min)
 - Application settings configured in src/main/resources/application.conf

##### System tests
Automated systems tests confirm that the end-to-end system functions as specified. (30 min)
 - Cucumber end to end tests to validate requests and values set on Redis
 - ScalaTest unit tests for functionality

##### Platform - The software build and tests pass on a modern Linux or Mac OS distribution (2 hours)


### Improvements
- Test the Scala Redis non-blocking library. Not included on the recommended list from Redis but could improve performance
- Improve the set and get time for the LFU cache implementation
- Environment specific configurations in tests and application
