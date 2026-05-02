package com.cheapradar.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;

@Data
@Entity
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tickets")
public class Ticket implements Comparable<Ticket> {
    @Id
    @GeneratedValue
    private BigInteger id;

    private String airportFrom;
    private String airportTo;
    private LocalDateTime date;
    private String provider;
    private BigDecimal price;
    private String link;

    private String airline;
    private String airlineLogo;

    @Override
    public int compareTo(Ticket other) {
        return price.compareTo(other.price);
    }
}
