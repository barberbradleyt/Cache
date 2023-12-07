public class Solution {
    public static void main(String[] args) {
        getAbsentKey();
        basicPutGetTest();
    }

    private static void getAbsentKey() {
        LFUCache<String, String> cache = new LFUCache<>(10, 10);
        LFUCache<String, Integer> cache2 = new LFUCache<>(10, 10);

        //Test retrieving an absent key - String value
        assert cache.size() == 0;
        assert cache.get("a").isEmpty();

        //Test retrieving an absent key - Integer value
        assert cache.size() == 0;
        assert cache2.get("a") == null;
    }

    private static void basicPutGetTest() {
        LFUCache<String, String> cache = new LFUCache<>(10, 10);

        //Test adding and retrieving entries
        cache.put("a", "A");
        cache.put("b", "B");
        cache.put("c", "C");
        cache.put("d", "D");
        cache.put("e", "E");

        assert cache.size() == 5;
        assert cache.get("a").equals("A");
        assert cache.get("b").equals("B");
        assert cache.get("c").equals("C");
        assert cache.get("d").equals("D");
        assert cache.get("e").equals("E");
    }
}
