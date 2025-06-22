package com.bingyu.holidays.client;

import com.bingyu.holidays.model.Holiday;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
public class NagerDateApiClient implements HolidayApiClient{

    public static final String CACHE_NAME = "holidays";
    public static final String RETRY_INSTANCE = "nagerApi";

    private final WebClient webClient;
    private final String holidaysPath;

    public NagerDateApiClient(WebClient.Builder builder,
                              @Value("${nager.api.base-url}") String baseUrl,
                              @Value("${nager.api.holidays-path}") String holidaysPath)  {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.holidaysPath  = holidaysPath;
    }

    @Override
    @Cacheable(value = CACHE_NAME, key = "#year + '-' + #countryCode", unless = "#result == null || #result.isEmpty()")
    @Retry(name = RETRY_INSTANCE, fallbackMethod = "fallbackHolidays")
    @RateLimiter(name = RETRY_INSTANCE)
    public List<Holiday> fetchHolidays(int year, String countryCode) {
        return webClient.get()
                .uri(holidaysPath, year, countryCode)
                .retrieve()
                .bodyToFlux(Holiday.class)
                .collectList()
                .block();
    }

    private List<Holiday> fallbackHolidays(int year, String countryCode, Throwable t) {
        log.error("Error fetching {} holidays for {}: {}", year, countryCode, t.getMessage());
        return Collections.emptyList();
    }
}
