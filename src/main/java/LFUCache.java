import java.lang.ref.SoftReference;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

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
    private int maxSize;
    private long expiryTimeInMillis;
    private Map<K, SoftReference<V>> keyValueMap;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs a new {@code LFUCache} instance with the specified expiry time and max size.
     *
     * @param expiryTimeInSeconds   the elapsed time since last access after which an entry should expire and be removed
     *                              from the cache
     * @param maxCacheSize          the maximum number of entries to be maintained in the cache
     */
    LFUCache(long expiryTimeInSeconds, int maxCacheSize) {
        expiryTimeInMillis = expiryTimeInSeconds * 1000;
        maxSize = maxCacheSize;
        keyValueMap = new ConcurrentHashMap<>(maxCacheSize);
    }

    /**
     * Returns the value associated with the provided {@code key}, or {@code null} if there is no mapping for the
     * {@code key} in this cache.
     *
     * @param key   the key whose associated value is to be returned
     * @return      the value associated with the provided {@code key}, else null if no association present
     */
    @Override
    public V get(K key) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(keyValueMap.get(key)).map(SoftReference::get).orElse(null);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Associates the provided {@value} with the provided {@code key} in this cache. If this cache already contains a
     * value associated with the provided {@code key} the old value will be overwritten with the provided {@code value}.
     * If adding this association will result in the entries maintained in this cache exceeding the cache size limit,
     * the least frequently used (LFU) entry will be removed to stay within the size limit.
     * <p>
     * Neither the key nor the value can be null.
     *
     * @param key   the key to which the provided value is to be associated
     * @param value the value to be associated with the provided key
     *
     * @throws NullPointerException if the provided key or value is null
     */
    @Override
    public void put(K key, V value) {
        if (key == null || value == null) {
            throw new NullPointerException();
        }

        lock.writeLock().lock();
        try {
            if (keyValueMap.size() == maxSize) {
                removeLFUEntry();
            }

            SoftReference<V> valueReference = new SoftReference<>(value);
            keyValueMap.put(key, valueReference);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all entries from this cache.
     */
    @Override
    public void clear() {
        keyValueMap.clear();
    }

    /**
     * Returns the number of entries in this cache.
     */
    @Override
    public int size() {
        return keyValueMap.size();
    }

    private void removeLFUEntry() {
        //TODO: compute LFU entry and remove
    }

    //TODO: add expiry mechanism
}
