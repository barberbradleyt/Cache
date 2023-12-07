import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
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
    private final static int CLEANUP_THREAD_PERIOD_MILLIS = 500;
    private final int maxSize;
    private final long expiryTimeInMillis;
    //mapping of key to associated value
    private final Map<K, LFUCacheEntry<K,V>> keyValueMap;
    //map of use frequency to list of value entries
    private final ConcurrentSkipListMap<Integer, DoublyLinkedLFUCacheEntryList<K, V>> frequencyEntryMap = new ConcurrentSkipListMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Constructs a new {@code LFUCache} instance with the specified expiry time and max size.
     *
     * @param expiryTimeInSeconds   the elapsed time since entry was added after which the entry should expire and be removed
     *                              from the cache
     * @param maxCacheSize          the maximum number of entries to be maintained in the cache
     */
    LFUCache(long expiryTimeInSeconds, int maxCacheSize) {
        if (expiryTimeInSeconds <= 0 || maxCacheSize <= 0) {
            throw new IllegalArgumentException("expiryTimeInSeconds and maxCacheSize must both be positive, non-zero values");
        }

        expiryTimeInMillis = expiryTimeInSeconds * 1000;
        maxSize = maxCacheSize;
        keyValueMap = new ConcurrentHashMap<>(maxCacheSize);

        createAndStartExpiryCleanupThread();
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
            LFUCacheEntry<K,V> entry = keyValueMap.get(key);
            if (entry == null) return null;

            int prevFrequency = entry.getHitFrequency();
            removeEntryFromFrequencyList(prevFrequency, entry);
            keyValueMap.computeIfPresent(key, (k, v) -> v.setHitFrequency(prevFrequency + 1));
            frequencyEntryMap.computeIfAbsent(prevFrequency + 1, entryList -> new DoublyLinkedLFUCacheEntryList<>()).add(entry);

            return entry.value();
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
            if (!keyValueMap.containsKey(key)) { //adding new entry
                LFUCacheEntry<K, V> entryToAdd = new LFUCacheEntry<>(key, value, 1);

                if (keyValueMap.size() == maxSize) { //need to remove LFU entry
                    int leastFrequency = frequencyEntryMap.firstKey();
                    LFUCacheEntry<K,V> entryToRemove = frequencyEntryMap.get(leastFrequency).head();

                    removeEntryFromFrequencyList(leastFrequency, entryToRemove);
                    keyValueMap.remove(entryToRemove.key());
                }

                frequencyEntryMap.computeIfAbsent(1, entryList -> new DoublyLinkedLFUCacheEntryList<>()).add(entryToAdd);
                keyValueMap.put(key, entryToAdd);
            }
            else { //updating existing entry
                LFUCacheEntry<K,V> entryToUpdate = keyValueMap.get(key);
                int prevFrequency = entryToUpdate.getHitFrequency();

                removeEntryFromFrequencyList(prevFrequency, entryToUpdate);
                keyValueMap.computeIfPresent(key, (k, v) -> v.setValue(value).setHitFrequency(prevFrequency + 1));
                frequencyEntryMap.computeIfAbsent(prevFrequency + 1, entryList -> new DoublyLinkedLFUCacheEntryList<>()).add(entryToUpdate);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all entries from this cache.
     */
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            keyValueMap.clear();
            frequencyEntryMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the number of entries in this cache.
     */
    @Override
    public int size() {
        return keyValueMap.size();
    }

    private void removeEntryFromFrequencyList(int frequency, LFUCacheEntry<K,V> entry) {
        frequencyEntryMap.get(frequency).remove(entry);
        if (frequencyEntryMap.get(frequency).size() == 0) {
            frequencyEntryMap.remove(frequency);
        }
    }

    private void createAndStartExpiryCleanupThread() {
        Thread expiryCleanupThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(CLEANUP_THREAD_PERIOD_MILLIS);
                    long currentTime = System.currentTimeMillis();
                    Map<Integer, List<LFUCacheEntry<K,V>>> frequencyExpiredEntriesMap = new HashMap<>();
                    for (LFUCacheEntry<K,V> entry : keyValueMap.values()) {
                        if (entry.getTimestamp() + expiryTimeInMillis <= currentTime) {
                            frequencyExpiredEntriesMap.computeIfAbsent(entry.getHitFrequency(), entryList -> new ArrayList<>()).add(entry);
                        }
                    }
                    frequencyExpiredEntriesMap.forEach((frequency, entries) -> {
                        for (LFUCacheEntry<K,V> entry : entries) {
                            removeEntryFromFrequencyList(frequency, entry);
                            keyValueMap.remove(entry.key());
                        }
                    });

                    keyValueMap.entrySet().removeIf(entry -> entry.getValue().getTimestamp() >= currentTime + expiryTimeInMillis);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        expiryCleanupThread.setDaemon(true);
        expiryCleanupThread.start();
    }
}

class LFUCacheEntry<K, V> {
    private final K key;
    private V value;
    private int hitFrequency;
    private final long timestamp;
    LFUCacheEntry<K,V> next;
    LFUCacheEntry<K,V> prev;

    public LFUCacheEntry(K key, V value, int hitFrequency) {
        this.key = key;
        this.value = value;
        this.hitFrequency = hitFrequency;
        timestamp = System.currentTimeMillis();
    }

    public K key() {
        return key;
    }

    public V value() {
        return value;
    }

    public LFUCacheEntry<K,V> setValue(V value) {
        this.value = value;
        return this;
    }

    public int getHitFrequency() {
        return hitFrequency;
    }

    public LFUCacheEntry<K,V> setHitFrequency(int hitFrequency) {
        this.hitFrequency = hitFrequency;
        return this;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

class DoublyLinkedLFUCacheEntryList<K, V> {
    private int size;
    private LFUCacheEntry<K,V> head;
    private LFUCacheEntry<K,V> tail;

    public LFUCacheEntry<K, V> head() {
        return head;
    }

    public int size() {
        return size;
    }

    public void add(LFUCacheEntry<K,V> newEntry) {
        if (head == null) {
            head = newEntry;
        } else {
            tail.next = newEntry;
            newEntry.prev = tail;
        }

        tail = newEntry;
        size++;
    }

    public void remove(LFUCacheEntry<K,V> entry) {
        //left side of entry
        if (entry.prev == null) {
            head = entry.next;
        }
        else {
            entry.prev.next = entry.next;
        }

        //right side of entry
        if (entry.next == null) {
            tail = entry.prev;
        }
        else {
            entry.next.prev = entry.prev;
        }

        size--;
    }
}
