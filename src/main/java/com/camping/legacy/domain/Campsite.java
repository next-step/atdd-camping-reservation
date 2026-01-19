package com.camping.legacy.domain;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "campsites")
@Getter
@Setter
@NoArgsConstructor
public class Campsite {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;
    
    @Column(unique = true, nullable = false)
    private String siteNumber;
    
    private String description;
    
    private Integer maxPeople;
    
    @OneToMany(mappedBy = "campsite", cascade = CascadeType.ALL)
    private List<Reservation> reservations = new ArrayList<>();

    @Builder
    private Campsite(String siteNumber, String description, Integer maxPeople) {
        this.siteNumber = siteNumber;
        this.description = description;
        this.maxPeople = maxPeople;
    }
}