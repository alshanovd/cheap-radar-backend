package com.cheapradar.backend.client.dto;

import com.cheapradar.backend.dto.search.TicketResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClientSearchResponse {
    List<TicketResponse> tickets;
}
