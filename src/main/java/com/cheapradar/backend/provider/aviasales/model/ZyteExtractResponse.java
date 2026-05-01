package com.cheapradar.backend.provider.aviasales.model;

import lombok.Data;

@Data
public class ZyteExtractResponse {
    private String url;
    private Integer statusCode;
    private String browserHtml;
}
