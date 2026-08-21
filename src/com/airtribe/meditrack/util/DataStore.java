package com.airtribe.meditrack.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class DataStore<T> {

    private final Map<String, T> records = new LinkedHashMap<>();
    private final Function<T, String> idExtractor;


    public DataStore(Function<T, String> idExtractor) {
        this.idExtractor = idExtractor;
    }

    public void save(T item) {
        records.put(idExtractor.apply(item), item);
    }

    public T findById(String id) {
        return records.get(id);
    }

    public boolean deleteById(String id) {
        return records.remove(id) != null;
    }

    public List<T> findAll() {
        return new ArrayList<>(records.values());
    }

    public int size() {
        return records.size();
    }

    public boolean exists(String id) {
        return records.containsKey(id);
    }
}