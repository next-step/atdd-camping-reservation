package com.camping.legacy.fake;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.repository.CampsiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class FakeCampsiteRepository implements CampsiteRepository {

    private final ConcurrentHashMap<Long, Campsite> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Campsite save(Campsite entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Campsite> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Campsite> findBySiteNumber(String siteNumber) {
        return store.values().stream()
                .filter(c -> c.getSiteNumber().equals(siteNumber))
                .findFirst();
    }

    @Override
    public Optional<Campsite> findBySiteNumberWithLock(String siteNumber) {
        return findBySiteNumber(siteNumber);
    }

    @Override
    public List<Campsite> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
