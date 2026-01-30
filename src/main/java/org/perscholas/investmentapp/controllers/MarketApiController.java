package org.perscholas.investmentapp.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/market")
public class MarketApiController {

    @Value("${market.api.key}")
    private String apiKey;

    @Value("${market.api.host:apidojo-yahoo-finance-v1.p.rapidapi.com}")
    private String apiHost;

    private final RestTemplate restTemplate = new RestTemplate();

    // existing: quotes proxy
    @GetMapping("/quotes")
    public ResponseEntity<String> getQuotes(
            @RequestParam(defaultValue = "US") String region,
            @RequestParam String symbols
    ) {
        String url = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/market/v2/get-quotes"
                + "?region=" + region
                + "&symbols=" + symbols;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", apiKey);
        headers.set("X-RapidAPI-Host", apiHost);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (Exception e) {
            log.error("MarketApiController.getQuotes failed: region={}, symbols={}", region, symbols, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"quotes request failed\"}");
        }
    }

    // add: news proxy (this fixes your 404)
    @GetMapping("/news")
    public ResponseEntity<String> getNews(
            @RequestParam(defaultValue = "generalnews") String category,
            @RequestParam(defaultValue = "US") String region
    ) {
        String url = "https://apidojo-yahoo-finance-v1.p.rapidapi.com/news/list"
                + "?category=" + category
                + "&region=" + region;

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RapidAPI-Key", apiKey);
        headers.set("X-RapidAPI-Host", apiHost);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        try {
            return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (Exception e) {
            log.error("MarketApiController.getNews failed: category={}, region={}", category, region, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("{\"error\":\"news request failed\"}");
        }
    }
}
