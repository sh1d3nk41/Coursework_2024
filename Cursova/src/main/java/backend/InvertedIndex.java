package backend;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InvertedIndex {
    // Потокобезпечна структура для зберігання індексу
    private final Map<String, Set<String>> index;

    public InvertedIndex() {
        this.index = new ConcurrentHashMap<>();
    }

    /**
     * Додає слово до індексу, пов'язуючи його з певним документом.
     * @param word Слово для індексації.
     * @param document Назва документа, в якому знайдено слово.
     */
    public void add(String word, String document) {
        index.computeIfAbsent(word.toLowerCase(), k -> Collections.synchronizedSet(new HashSet<>())).add(document);
    }

    /**
     * Пошук документів за ключовим словом.
     * @param word Слово для пошуку.
     * @return Список документів, що містять слово.
     */
    public Set<String> search(String word) {
        return index.getOrDefault(word.toLowerCase(), Collections.emptySet());
    }


    /**
     * Очищення всього індексу.
     */
    public void clear() {
        index.clear();
    }

    /**
     * Отримати всю структуру індексу (для тестування).
     * @return Повна структура індексу.
     */
    public Map<String, Set<String>> getIndex() {
        return index;
    }
}
