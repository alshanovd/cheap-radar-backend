package com.cheapradar.backend.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.time.ZonedDateTime;

@Data
@Entity
@Builder
@Table(name = "settings")
@AllArgsConstructor
public class Setting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private ZonedDateTime createdAt;
    private BigInteger userId;
    private String currency;
    private boolean notifications;

    public Setting() {
        this.createdAt = ZonedDateTime.now();
    }

}
