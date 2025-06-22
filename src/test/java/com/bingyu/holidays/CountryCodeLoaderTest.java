package com.bingyu.holidays;

import com.bingyu.holidays.config.CountryCodeLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CountryCodeLoaderTest {
    private WebClient.Builder mockBuilder;
    private WebClient mockWebClient;
    private WebClient.RequestHeadersUriSpec    mockUriSpec;
    private WebClient.RequestHeadersSpec      mockReqSpec;
    private WebClient.ResponseSpec            mockRespSpec;
    private CountryCodeLoader loader;

    @BeforeEach
    void setUp() {
        mockBuilder   = mock(WebClient.Builder.class);
        mockWebClient = mock(WebClient.class);
        mockUriSpec   = mock(WebClient.RequestHeadersUriSpec.class);
        mockReqSpec   = mock(WebClient.RequestHeadersSpec.class);
        mockRespSpec  = mock(WebClient.ResponseSpec.class);

        loader = new CountryCodeLoader(mockBuilder);
        ReflectionTestUtils.setField(loader, "baseUrl", "http://api.test");
        ReflectionTestUtils.setField(loader, "countriesPath", "/available");

        when(mockBuilder.baseUrl(eq("http://api.test"))).thenReturn(mockBuilder);
        when(mockBuilder.build()).thenReturn(mockWebClient);
        when(mockWebClient.get()).thenReturn(mockUriSpec);
        when(mockUriSpec.uri(eq("/available"))).thenReturn(mockReqSpec);
        when(mockReqSpec.retrieve()).thenReturn(mockRespSpec);
    }

    @Test
    void testInitLoadsCodesAndIsValid() {
        CountryCodeLoader.AvailableCountry c1 = new CountryCodeLoader.AvailableCountry();
        c1.setKey("US");
        CountryCodeLoader.AvailableCountry c2 = new CountryCodeLoader.AvailableCountry();
        c2.setKey("DE");
        @SuppressWarnings("unchecked")
        Flux<CountryCodeLoader.AvailableCountry> flux = mock(Flux.class);
        when(mockRespSpec.bodyToFlux(CountryCodeLoader.AvailableCountry.class)).thenReturn(flux);
        Mono<List<CountryCodeLoader.AvailableCountry>> mono = Mono.just(List.of(c1, c2));
        when(flux.collectList()).thenReturn(mono);

        loader.init();

        assertTrue(loader.isValid("US"), "should contain US");
        assertTrue(loader.isValid("DE"), "should contain DE");
        assertFalse(loader.isValid("FR"), "shouldn't contain FR");
    }

    @Test
    void testInitWithEmptyList() {
        @SuppressWarnings("unchecked")
        Flux<CountryCodeLoader.AvailableCountry> flux = mock(Flux.class);
        when(mockRespSpec.bodyToFlux(CountryCodeLoader.AvailableCountry.class)).thenReturn(flux);
        when(flux.collectList()).thenReturn(Mono.just(List.of()));

        loader.init();
        assertFalse(loader.isValid("ANY"), " should return false");
    }

    @Test
    void testInitThrowsOnError() {

        when(mockRespSpec.bodyToFlux(CountryCodeLoader.AvailableCountry.class))
                .thenThrow(new RuntimeException("network error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> loader.init());
        assertEquals("network error", ex.getMessage());
    }
}
