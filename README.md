# Cache
This repo contains a __Cache__ interface as well as an implementation, __LFUCache__, which implements an in-memory cache
that uses a Least Frequently Used (LFU) algorithm for entry eviction. Additionally, to reduce memory usage, this
implementation also has a configurable expiry time after which entries are evicted from the cache. This expiry time is
the time elapsed since the entry was _added_ to the cache.

## LFUCache Design
Since the main purpose of a cache is to reduce data retrieval time, I prioritized the performance of the _get()_ and 
_put()_ operations over memory usage. These operations perform in O(1) average time complexity.

Expiry eviction is performed by a separate thread, so this implementation has also been designed to be thread-safe and
synchronized by way of concurrent classes and read/write locks.

The expiry eviction thread runs at a fixed period of 500ms regardless of the expiry time. An alternative would be to 
have the expiry eviction thread run at a period proportional to the expiry time (ie. expiry time / n : 1 <= n), however 
this would mean the maximum lag between an entry expiring and being evicted would increase as the expiry time increased. 
With a constant period the maximum lag is instead constant - in this case, 500ms.

All method naming as well as behaviour for null keys, null values and attempted retrieval of keys not present in the 
cache all follow the naming conventions and behaviour of the underlying [ConcurrentHashMap](https://docs.oracle.com/en/java/javase/20/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) used to store the entries.

### Issues
One issue with this implementation is that, while the _size()_ anf _get()_ methods are O(1), this comes at the cost of 
expiry accuracy since they do not account for the lag between entries expiring and being evicted by the expiry eviction 
thread. This can result in entries which have expired but not yet evicted being considered. An alternative would be to 
filter out entries for which the timestamp is older than the expiry time. This would result in a more accurate expiry behaviour,
however it would come at the expense of the _size()_ method increasing in time complexity to O(n). Due to this, I 
decided the expiry accuracy reduction (since this is just to conserve memory usage) was worth it to keep the average 
time complexity of all methods to O(1).

### Testing
As requested, tests have been written within the _main_ method of the __Solution__ class. Typically, I would use a test
framework, such as JUnit, to perform this kind of testing. Each test performs on both a cache using String values as well
one using Integer values. This was intended to ensure nullable (Integer, etc.) and non-nullable (String) objects can be
handled by this implementation.