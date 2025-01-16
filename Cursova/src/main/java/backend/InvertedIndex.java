package backend;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InvertedIndex {
    private final Map<String, Set<String>> index;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }

    public void add(String word, String document) {
        index.computeIfAbsent(word.toLowerCase(), k -> Collections.synchronizedSet(new HashSet<>())).add(document);
    }

    public Set<String> search(String word) {
        return index.getOrDefault(word.toLowerCase(), Collections.emptySet());
    }


    public void clear() {
        index.clear();
    }

    public Map<String, Set<String>> getIndex() {
        return index;
    }
}
