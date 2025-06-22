package com.bingyu.holidays.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class CountryCodeLoader {
    private WebClient.Builder webClientBuilder = WebClient.builder();
    private WebClient webClient;
    private volatile Set<String> codes = Collections.emptySet();

    @Value("${nager.api.base-url}")
    private String baseUrl;

    @Value("${nager.api.available-countries-path}")
    private String countriesPath;

    public CountryCodeLoader(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        try {
            loadCodes();
        } catch (Exception e) {
            log.error("Failed to load country codes, continuing with empty set", e);
            throw e;
        }
    }

    private void loadCodes() {
        List<AvailableCountry> list = webClient.get()
                .uri(countriesPath)
                .retrieve()
                .bodyToFlux(AvailableCountry.class)
                .collectList()
                .block();
        Set<String> loaded = new HashSet<>();
        if (list != null) {
            for (AvailableCountry c : list) {
                loaded.add(c.getKey());
            }
        }
        codes = Collections.unmodifiableSet(loaded);
    }

    public boolean isValid(String code) {
        return codes != null && codes.contains(code);
    }

    @Data
    public static class AvailableCountry {
        @JsonProperty("countryCode")
        private String key;
    }
}
