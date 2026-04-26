package com.cheapradar.backend.service;

import com.cheapradar.backend.model.Search;
import com.cheapradar.backend.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final EmailProperties properties;

    public void sendSearchResultEmail(User user, Search search) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("Cheap Radar <%s>".formatted(properties.getSender()));
            message.setTo(user.getEmail());
            message.setSubject("Your Search Results");
            message.setText("Here are your latest search results:\n" + search.toString());
            log.info("Sending search results email to {}", user.getEmail());
            mailSender.send(message);
            log.info("Email sent successfully to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", user.getEmail(), e.getMessage(), e);
        }
    }
}
