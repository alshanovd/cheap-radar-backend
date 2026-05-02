package com.cheapradar.backend.service;

import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.Ticket;
import com.cheapradar.backend.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

import java.time.format.DateTimeFormatter;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final String DATE_PATTERN = "dd MMM yyyy";
    private static final String TIME_PATTERN = "HH:mm";

    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public void sendSearchResultEmail(User user, Search search) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern(TIME_PATTERN);

            helper.setFrom(properties.getSender());
            helper.setTo(user.getEmail());
            helper.setSubject("Your Search Results");
            StringBuilder html = new StringBuilder();
            html.append("<html><body>");
            html.append("<h2><a href='")
                .append(properties.getLookupsUrl())
                .append(search.getId())
                .append("' target='_blank' rel='noopener noreferrer' style='color: #2a7ae2; text-decoration: underline;'>Your Latest Search Results</a></h2>");
            html.append("<table border='1' cellpadding='8' cellspacing='0' style='border-collapse:collapse;'>");
            html.append("<tr><th>AIRLINE</th><th>DATE</th><th>TIME</th><th>FROM</th><th>TO</th><th>PROVIDER</th><th>PRICE</th><th>LINK</th></tr>");
            if (search.getTickets() != null) {
                Collections.sort(search.getTickets());
                for (Ticket ticket : search.getTickets()) {
                    html.append("<tr>");
                    html.append("<td><img src='")
                        .append(ticket.getAirlineLogo() != null ? ticket.getAirlineLogo() : "")
                        .append("' alt='Logo' style='height:32px;vertical-align:middle;'/> ")
                        .append(ticket.getAirline() != null ? ticket.getAirline() : "-")
                        .append("</td>");
                    html.append("<td>").append(ticket.getDate() != null ? dateFormatter.format(ticket.getDate()) : "-").append("</td>");
                    html.append("<td>").append(ticket.getDate() != null ? timeFormatter.format(ticket.getDate()) : "-").append("</td>");
                    html.append("<td>").append(ticket.getAirportFrom() != null ? ticket.getAirportFrom() : "-").append("</td>");
                    html.append("<td>").append(ticket.getAirportTo() != null ? ticket.getAirportTo() : "-").append("</td>");
                    html.append("<td>").append(ticket.getProvider() != null ? ticket.getProvider() : "-").append("</td>");
                    html.append("<td>").append(ticket.getPrice() != null ? ticket.getPrice() : "-").append("</td>");
                    html.append("<td>");
                    if (ticket.getLink() != null && !ticket.getLink().isEmpty()) {
                        html.append("<a href='")
                            .append(ticket.getLink())
                            .append("' target='_blank' rel='noopener noreferrer'>Open</a>");
                    } else {
                        html.append("-");
                    }
                    html.append("</td>");
                    html.append("</tr>");
                }
            } else {
                html.append("<tr><td colspan='8'>No tickets found.</td></tr>");
            }
            html.append("</table>");
            html.append("</body></html>");
            helper.setText(html.toString(), true);
            log.info("Sending search results email to {}", user.getEmail());
            mailSender.send(message);
            log.info("Email sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }
}
