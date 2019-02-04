

Concurrency and blocking calls

The recommended Scala Redis client is by nature blocking. To mitigate that to increase performance see details here: https://github.com/debasishg/scala-redis#implementing-asynchronous-patterns-using-pooling-and-futures