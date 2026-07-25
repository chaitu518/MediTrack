package com.airtribe.meditrack.entity;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class MedicalEntity {
    protected static final AtomicInteger entityCount = new AtomicInteger(0);
    protected final String id;

    public MedicalEntity(String id) {
        entityCount.incrementAndGet();
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static AtomicInteger getEntityCount() {
        return entityCount;
    }
    public abstract String summary();

}