package com.bingyu.holidays;

import com.bingyu.holidays.dto.CountryHolidayCount;
import com.bingyu.holidays.dto.LastHolidayDTO;
import com.bingyu.holidays.service.HolidayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.junit.jupiter.api.DisplayName;
import org.springframework.http.*;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"spring.cache.type=none"}
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HolidayIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private HolidayService service;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public HolidayService service(){
            return mock(HolidayService.class);
        }
    }


    private String baseUrl() {
        return "http://localhost:" + port + "/holidays";
    }

    @Test
    @DisplayName("Integration: GET /holidays/last-three?country=US")
     void testLastThree() {
        List<LastHolidayDTO> mockList = List.of(
                new LastHolidayDTO(LocalDate.of(2025, 6, 18), "Queen's Birthday"),
                new LastHolidayDTO(LocalDate.of(2025, 6, 10), "Eid al-Adha"),
                new LastHolidayDTO(LocalDate.of(2025, 5,  1), "Labor Day")
        );
        given(service.getLastThreeHolidays("US")).willReturn(mockList);

        ResponseEntity<LastHolidayDTO[]> response = restTemplate
                .getForEntity(baseUrl() + "/last-three?country=US", LastHolidayDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LastHolidayDTO[] body = response.getBody();
        assertThat(body)
                .isNotNull()
                .hasSize(3);
        assertThat(body[0].getDate()).isEqualTo(LocalDate.of(2025, 6, 18));
        assertThat(body[0].getName()).isEqualTo("Queen's Birthday");
        assertThat(body[1].getDate()).isEqualTo(LocalDate.of(2025, 6, 10));
        assertThat(body[1].getName()).isEqualTo("Eid al-Adha");
        assertThat(body[2].getDate()).isEqualTo(LocalDate.of(2025, 5,  1));
        assertThat(body[2].getName()).isEqualTo("Labor Day");
    }

    @Test
    @DisplayName("Integration: GET /holidays/weekday-count?year=2025&countries=US,DE")
    void testWeekdayCount() {
        List<CountryHolidayCount> mockCounts = List.of(
                new CountryHolidayCount("DE", 12),
                new CountryHolidayCount("US", 10)
        );
        given(service.countWeekdayHolidays(2025, List.of("US", "DE"))).willReturn(mockCounts);

        ResponseEntity<CountryHolidayCount[]> response = restTemplate
                .getForEntity(baseUrl() + "/weekday-count?year=2025&countries=US,DE",
                        CountryHolidayCount[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        CountryHolidayCount[] body = response.getBody();
        assertThat(body)
                .isNotNull()
                .hasSize(2);
        assertThat(body[0].getCountryCode()).isEqualTo("DE");
        assertThat(body[0].getWeekdayHolidayCount()).isEqualTo(12);
        assertThat(body[1].getCountryCode()).isEqualTo("US");
        assertThat(body[1].getWeekdayHolidayCount()).isEqualTo(10);
    }

    @Test
    @DisplayName("Integration: GET /holidays/common?year=2025&countryA=US&countryB=DE")
    void testCommon() {
        List<LastHolidayDTO> commons = List.of(
                new LastHolidayDTO(LocalDate.of(2025, 12, 25), "Christmas Day")
        );
        given(service.findCommonHolidays(2025, "US", "DE")).willReturn(commons);

        ResponseEntity<LastHolidayDTO[]> response = restTemplate
                .getForEntity(baseUrl()
                                + "/common?year=2025&countryA=US&countryB=DE",
                        LastHolidayDTO[].class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LastHolidayDTO[] body = response.getBody();
        assertThat(body)
                .isNotNull()
                .hasSize(1);
        assertThat(body[0].getDate()).isEqualTo(LocalDate.of(2025, 12, 25));
        assertThat(body[0].getName()).isEqualTo("Christmas Day");
    }

}
