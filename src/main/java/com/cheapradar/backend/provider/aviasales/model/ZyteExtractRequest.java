package com.cheapradar.backend.provider.aviasales.model;

import java.util.List;
import java.util.Map;

public record ZyteExtractRequest(String url, boolean browserHtml, boolean javascript, List<Map<String, Object>> actions) {
}
