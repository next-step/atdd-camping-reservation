package com.camping.legacy.fake;

import com.camping.legacy.domain.Campsite;
import com.camping.legacy.domain.Reservation;
import com.camping.legacy.repository.ReservationRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class FakeReservationRepository implements ReservationRepository {

    private final ConcurrentHashMap<Long, Reservation> store = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Reservation save(Reservation entity) {
        if (entity.getId() == null) {
            entity.setId(idGenerator.getAndIncrement());
        }
        if (entity.getStatus() == null) {
            entity.setStatus("CONFIRMED");
        }
        store.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<Reservation> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public List<Reservation> findByCustomerName(String customerName) {
        return store.values().stream()
                .filter(r -> r.getCustomerName().equals(customerName))
                .collect(Collectors.toList());
    }

    @Override
    public List<Reservation> findByCustomerNameAndPhoneNumber(String customerName, String phoneNumber) {
        return store.values().stream()
                .filter(r -> r.getCustomerName().equals(customerName))
                .filter(r -> phoneNumber.equals(r.getPhoneNumber()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCampsiteAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Campsite campsite, LocalDate endDate, LocalDate startDate) {
        return store.values().stream()
                .filter(r -> r.getCampsite().getId().equals(campsite.getId()))
                .anyMatch(r -> !r.getStartDate().isAfter(endDate) && !r.getEndDate().isBefore(startDate));
    }

    @Override
    public boolean existsByCampsiteAndReservationDate(Campsite campsite, LocalDate date) {
        return store.values().stream()
                .filter(r -> r.getCampsite().getId().equals(campsite.getId()))
                .anyMatch(r -> date.equals(r.getReservationDate()));
    }

    @Override
    public void deleteAll() {
        store.clear();
    }
}
