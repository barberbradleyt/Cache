/**
 * An in-memory mapping of keys to values. Entries are manually added using {@link #put(Object, Object)} and can be
 * retrieved using {@link #get(Object)}.
 *
 * @param <K>   the type of keys used by this cache
 * @param <V>   the type of values used by this cache
 */
public interface Cache<K, V> {
    /**
     * Returns the value associated with the {@code key} in this cache or {@code null} if there is no
     * value for the {@code key}.
     *
     * @param key   the key whose associated value is to be returned
     **/
    public V get(K key);

    /**
     * Associates the {@code value} with the {@code key} in this cache. If the {@code key} is already associated with a
     * value in this cache, the old value is overwritten with the provided {@code value}.
     *
     * @param key   the key to which the provided value is to be associated
     * @param value the value to be associated with the provided key
     */
    public void put(K key, V value);
}
