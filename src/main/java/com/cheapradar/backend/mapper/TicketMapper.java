package com.cheapradar.backend.mapper;

import com.cheapradar.backend.dto.search.TicketResponse;
import com.cheapradar.backend.model.Ticket;
import org.springframework.stereotype.Component;

@Component
public class TicketMapper {
    public Ticket map(TicketResponse ticketResponse) {
        Ticket ticket = new Ticket();
        ticket.setProvider(ticketResponse.getProvider());
        ticket.setPrice(ticketResponse.getPrice());
        ticket.setAirportFrom(ticketResponse.getAirportFrom());
        ticket.setAirportTo(ticketResponse.getAirportTo());
        ticket.setDate(ticketResponse.getDate());
        ticket.setLink(ticketResponse.getLink());
        return ticket;
    }
}
