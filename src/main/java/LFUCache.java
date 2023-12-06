import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory mapping of keys to values. Entries are manually added using {@link #put(Object, Object)} and can be
 * retrieved using {@link #get(Object)}.
 * <p>
 * When adding an entry will exceed the cache size limit, the least frequently used (LFU) entry will be automatically
 * removed.
 * <p>
 * Additionally, entries will expire if not accessed within a provided expiry time limit. Expired entries are
 * automatically removed from the cache.
 *
 * @param <K>   the type of keys used by this cache
 * @param <V>   the type of values used by this cache
 */
public class LFUCache<K, V> implements Cache<K, V> {
    int maxCacheSize;
    long expiryTimeInMillis;
    Map<K, V> keyValueMap = new HashMap<>();

    /**
     * Constructs a new {@code LFUCache} instance with the specified expiry time and max size.
     *
     * @param expiryTimeInSeconds   the elapsed time since last access after which an entry should expire and be removed
     *                              from the cache
     * @param maxCacheSize          the maximum number of entries to be maintained in the cache
     */
    LFUCache(long expiryTimeInSeconds, int maxCacheSize) {
        expiryTimeInMillis = expiryTimeInSeconds * 1000;
        this.maxCacheSize = maxCacheSize;
    }

    /**
     * Returns the value associated with the provided {@code key}, or {@code null} if there is no mapping for the
     * {@code key} in this cache.
     *
     * @param key   the key whose associated value is to be returned
     * @return      the value associated with the provided {@code key}
     */
    public V get(K key) {
        return keyValueMap.get(key);
    }

    /**
     * Associates the provided {@value} with the provided {@code key} in this cache. If this cache already contains a
     * value associated with the provided {@code key} the old value will be overwritten with the provided {@code value}.
     * If adding this association will result in the entries maintained in this cache exceeding the cache size limit,
     * the least frequently used (LFU) entry will be removed to stay within the size limit.
     *
     * @param key   the key to which the provided value is to be associated
     * @param value the value to be associated with the provided key
     */
    public void put(K key, V value) {
        //TODO: if current size == size limit, compute LFU entry and remove
        keyValueMap.put(key, value);
    }

    //TODO: add expiry mechanism
}
