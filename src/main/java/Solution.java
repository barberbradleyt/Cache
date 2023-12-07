import java.util.Objects;

public class Solution {
    public static void main(String[] args) {
        getAbsentKey();
        basicPutGetTest();
        LFUTest();
    }

    private static void getAbsentKey() {
        System.out.println("Running getAbsentKey() test...");

        LFUCache<String, String> cache = new LFUCache<>(10, 10);
        LFUCache<String, Integer> cache2 = new LFUCache<>(10, 10);

        try {
            //Test retrieving an absent key - String value
            assertEquals(cache.size(), 0);
            assertEquals(cache.get("a"), null);

            //Test retrieving an absent key - Integer value
            assertEquals(cache2.size(), 0);
            assertEquals(cache2.get("a"), null);
        } catch (IllegalStateException e) {
            System.out.printf("Test FAILED: %s%n", e.getMessage());
            return;
        }
        System.out.println("Test SUCCEEDED");
    }

    private static void basicPutGetTest() {
        System.out.println("Running basicPutGetTest() test...");

        LFUCache<String, String> cache = new LFUCache<>(10, 10);
        LFUCache<String, Integer> cache2 = new LFUCache<>(10, 10);

        try {
            //Test adding and retrieving entries - String value
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            cache.put("d", "D");
            cache.put("e", "E");

            assertEquals(cache.size(), 5);
            assertEquals(cache.get("a"), "A");
            assertEquals(cache.get("b"), "B");
            assertEquals(cache.get("c"), "C");
            assertEquals(cache.get("d"), "D");
            assertEquals(cache.get("e"), "E");

            //Test adding and retrieving entries - Integer value
            cache2.put("a", 1);
            cache2.put("b", 2);
            cache2.put("c", 3);
            cache2.put("d", 4);
            cache2.put("e", 5);

            assertEquals(cache2.size(), 5);
            assertEquals(cache2.get("a"), 1);
            assertEquals(cache2.get("b"), 2);
            assertEquals(cache2.get("c"), 3);
            assertEquals(cache2.get("d"), 4);
            assertEquals(cache2.get("e"), 5);
        } catch (IllegalStateException e) {
            System.out.printf("Test FAILED: %s%n", e.getMessage());
            return;
        }

        System.out.println("Test SUCCEEDED");
    }

    private static void LFUTest() {
        System.out.println("Running LFUTest() test...");

        LFUCache<String, String> cache = new LFUCache<>(10, 5);
        LFUCache<String, Integer> cache2 = new LFUCache<>(10, 5);

        try {
            //Test adding and retrieving entries beyond max size - String value
            cache.put("a", "A");
            cache.put("b", "B");
            cache.put("c", "C");
            cache.put("d", "D");
            cache.put("e", "E");

            cache.get("b");
            cache.get("c");
            cache.get("d");

            cache.put("f", "F"); //a->A is oldest LFU, should be removed
            assertEquals(cache.size(), 5);
            assertEquals(cache.get("a"), null);
            assertEquals(cache.get("f"), "F");

            cache.put("g", "G"); //e->E is oldest LFU, should be removed
            assertEquals(cache.size(), 5);
            assertEquals(cache.get("e"), null);
            assertEquals(cache.get("g"), "G");

            //Test adding and retrieving entries beyond max size - Integer value
            cache2.put("a", 1);
            cache2.put("b", 2);
            cache2.put("c", 3);
            cache2.put("d", 4);
            cache2.put("e", 5);

            cache2.get("b");
            cache2.get("c");
            cache2.get("d");

            cache2.put("f", 6); //a->A is oldest LFU, should be removed
            assertEquals(cache2.size(), 5);
            assertEquals(cache2.get("a"), null);
            assertEquals(cache2.get("f"), 6);

            cache2.put("g", 7); //e->E is oldest LFU, should be removed
            assertEquals(cache2.size(), 5);
            assertEquals(cache2.get("e"), null);
            assertEquals(cache2.get("g"), 7);
        } catch (IllegalStateException e) {
            System.out.printf("Test FAILED: %s%n", e.getMessage());
            return;
        }

        System.out.println("Test SUCCEEDED");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new IllegalStateException(String.format("expected='%s'; actual='%s'", expected, actual));
        }
    }
}
