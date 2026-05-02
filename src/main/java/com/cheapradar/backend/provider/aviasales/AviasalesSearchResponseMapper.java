package com.cheapradar.backend.provider.aviasales;

import com.cheapradar.backend.provider.dto.ProviderSearchRequest;
import com.cheapradar.backend.provider.dto.ProviderSearchResponse;
import com.cheapradar.backend.provider.dto.ProviderTicket;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AviasalesSearchResponseMapper {
    private static final Pattern TIME_PATTERN = Pattern.compile("\\b(\\d{1,2}):(\\d{2})\\b");
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{1,2})\\s+([А-Яа-яЁё]+)");
    private static final Pattern AIRPORT_CODE_PATTERN = Pattern.compile("\\b[A-Z]{3}\\b");
    private static final Map<String, Integer> RUSSIAN_MONTHS = Map.ofEntries(
            Map.entry("января", 1),
            Map.entry("янв", 1),
            Map.entry("февраля", 2),
            Map.entry("фев", 2),
            Map.entry("марта", 3),
            Map.entry("мар", 3),
            Map.entry("апреля", 4),
            Map.entry("апр", 4),
            Map.entry("мая", 5),
            Map.entry("июня", 6),
            Map.entry("июн", 6),
            Map.entry("июля", 7),
            Map.entry("июл", 7),
            Map.entry("августа", 8),
            Map.entry("авг", 8),
            Map.entry("сентября", 9),
            Map.entry("сен", 9),
            Map.entry("сент", 9),
            Map.entry("октября", 10),
            Map.entry("окт", 10),
            Map.entry("ноября", 11),
            Map.entry("ноя", 11),
            Map.entry("декабря", 12),
            Map.entry("дек", 12)
    );

    private final AviasalesClientProperties properties;

    public ProviderSearchResponse map(List<AviasalesHtmlResponse> responses, ProviderSearchRequest request) {
        String provider = properties.getProvider().toLowerCase(Locale.ROOT);

        return ProviderSearchResponse.builder()
                .tickets(responses.stream()
                        .flatMap(response -> map(response, request, provider).stream())
                        .sorted()
                        .limit(properties.getMaximumTickets())
                        .toList())
                .build();
    }

    private List<ProviderTicket> map(AviasalesHtmlResponse response, ProviderSearchRequest request, String provider) {
        Document document = Jsoup.parse(normalize(response.html()));

        return priceElementsBeforeSoftTickets(document).stream()
                .map(priceElement -> mapTicket(priceElement, response, request, provider))
                .flatMap(Optional::stream)
                .toList();
    }

    private List<Element> priceElementsBeforeSoftTickets(Document document) {
        return document.getAllElements().stream()
                .takeWhile(element -> !"soft-tickets-informer".equals(element.attr("data-test-id")))
                .filter(element -> "price".equals(element.attr("data-test-id")))
                .toList();
    }

    private Optional<ProviderTicket> mapTicket(
            Element priceElement,
            AviasalesHtmlResponse response,
            ProviderSearchRequest request,
            String provider
    ) {
        Element ticket = findTicket(priceElement, response.searchDate()).orElse(priceElement);
        Optional<BigDecimal> price = parsePrice(priceElement.text());
        Optional<LocalDateTime> departureDateTime = parseDepartureDateTime(ticket, response.searchDate());

        if (price.isEmpty() || departureDateTime.isEmpty()) {
            return Optional.empty();
        }

        String link = findLink(ticket).orElse(response.searchUrl());
        String airlineLogo = findAirlineLogo(ticket).orElse(null);

        return Optional.of(ProviderTicket.builder()
                .provider(provider)
                .airportFrom(findAirportFrom(ticket, request))
                .airportTo(findAirportTo(ticket, request))
                .date(departureDateTime.get())
                .price(price.get())
                .link(link)
                .airline(findAirline(ticket).orElse(null))
                .airlineLogo(airlineLogo)
                .build());
    }

    private Optional<Element> findTicket(Element priceElement, LocalDate searchDate) {
        Element current = priceElement;
        while (current != null) {
            if (isTicketScope(current, searchDate)) {
                return Optional.of(current);
            }
            current = current.parent();
        }
        return Optional.empty();
    }

    private boolean isTicketScope(Element element, LocalDate searchDate) {
        return countPrices(element) == 1
                && element.select("img[alt][src]").size() > 0
                && parseDepartureDateTime(element, searchDate).isPresent();
    }

    private long countPrices(Element element) {
        return element.getAllElements().stream()
                .filter(candidate -> "price".equals(candidate.attr("data-test-id")))
                .count();
    }

    private Optional<BigDecimal> parsePrice(String text) {
        String normalized = cleanText(text)
                .replaceAll("[^\\d,.]", "")
                .replace(",", ".");
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(normalized));
    }

    private Optional<LocalDateTime> parseDepartureDateTime(Element ticket, LocalDate searchDate) {
        Optional<LocalTime> time = ticket.select("[data-test-id=text]").stream()
                .map(Element::text)
                .map(this::parseTime)
                .flatMap(Optional::stream)
                .findFirst();
        Optional<LocalDate> date = ticket.select("[data-test-id=text]").stream()
                .map(Element::text)
                .map(text -> parseDate(text, searchDate.getYear()))
                .flatMap(Optional::stream)
                .findFirst();

        if (time.isEmpty() || date.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(LocalDateTime.of(date.get(), time.get()));
    }

    private Optional<LocalTime> parseTime(String text) {
        Matcher matcher = TIME_PATTERN.matcher(cleanText(text));
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(LocalTime.of(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
        ));
    }

    private Optional<LocalDate> parseDate(String text, int year) {
        Matcher matcher = DATE_PATTERN.matcher(cleanText(text).toLowerCase(Locale.ROOT));
        if (!matcher.find()) {
            return Optional.empty();
        }

        Integer month = RUSSIAN_MONTHS.get(matcher.group(2));
        if (month == null) {
            return Optional.empty();
        }

        return Optional.of(LocalDate.of(year, month, Integer.parseInt(matcher.group(1))));
    }

    private String findAirportFrom(Element ticket, ProviderSearchRequest request) {
        String requestedCode = request.getAirportFrom().toUpperCase(Locale.ROOT);
        return findAirportCodes(ticket).contains(requestedCode)
                ? requestedCode
                : request.getAirportFrom();
    }

    private String findAirportTo(Element ticket, ProviderSearchRequest request) {
        String requestedCode = request.getAirportTo().toUpperCase(Locale.ROOT);
        return findAirportCodes(ticket).contains(requestedCode)
                ? requestedCode
                : request.getAirportTo();
    }

    private Set<String> findAirportCodes(Element ticket) {
        Set<String> codes = new LinkedHashSet<>();
        ticket.select("[data-test-id=text]").forEach(element -> {
            Matcher matcher = AIRPORT_CODE_PATTERN.matcher(cleanText(element.text()));
            while (matcher.find()) {
                codes.add(matcher.group());
            }
        });
        return codes;
    }

    private Optional<String> findAirline(Element ticket) {
        return ticket.select("img[alt][src]").stream()
                .map(element -> element.attr("alt"))
                .filter(text -> !text.isBlank())
                .findFirst();
    }

    private Optional<String> findAirlineLogo(Element ticket) {
        return ticket.select("img[alt][src]").stream()
                .map(element -> normalizeUrl(element.attr("src")))
                .filter(text -> !text.isBlank())
                .findFirst();
    }

    private Optional<String> findLink(Element ticket) {
        return ticket.select("a[href]").stream()
                .map(element -> normalizeUrl(element.attr("href")))
                .filter(text -> !text.isBlank())
                .findFirst();
    }

    private String normalizeUrl(String url) {
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("/")) {
            return "https://www.aviasales.ru" + url;
        }
        return url;
    }

    private String normalize(String html) {
        return html.replace("\\\"", "\"");
    }

    private String cleanText(String text) {
        return text.replace('\u00A0', ' ')
                .replace('\u202F', ' ')
                .replace('\u2060', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public record AviasalesHtmlResponse(String html, String searchUrl, LocalDate searchDate) {
    }
}
