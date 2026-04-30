package com.cheapradar.backend.mapper;

import com.cheapradar.backend.model.Ticket;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public Ticket map(ProviderTicket providerTicket) {
        return Ticket.builder()
                .provider(providerTicket.getProvider())
                .price(providerTicket.getPrice())
                .airportFrom(providerTicket.getAirportFrom())
                .airportTo(providerTicket.getAirportTo())
                .date(providerTicket.getDate())
                .link(providerTicket.getLink())
                .airline(providerTicket.getAirline())
                .airlineLogo(providerTicket.getAirlineLogo())
                .build();
    }
}
