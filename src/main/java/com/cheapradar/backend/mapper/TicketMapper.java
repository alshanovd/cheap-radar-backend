package com.cheapradar.backend.mapper;

import com.cheapradar.backend.dto.search.TicketResponse;
import com.cheapradar.backend.model.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public Ticket map(TicketResponse ticketResponse) {
        return Ticket.builder()
                .provider(ticketResponse.getProvider())
                .price(ticketResponse.getPrice())
                .airportFrom(ticketResponse.getAirportFrom())
                .airportTo(ticketResponse.getAirportTo())
                .date(ticketResponse.getDate())
                .link(ticketResponse.getLink())
                .airline(ticketResponse.getAirline())
                .airlineLogo(ticketResponse.getAirlineLogo())
                .build();
    }
}
