package com.airtribe.meditrack.interfaces;


import java.util.List;


public interface Searchable<T> {

    T searchById(String id);

    List<T> searchByName(String name);

    default boolean matches(String candidate, String query) {
        if (candidate == null || query == null) {
            return false;
        }
        return candidate.toLowerCase().contains(query.toLowerCase());
    }
}
