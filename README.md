# Accenture Holiday App

## Overview

This application provides a REST API to fetch and analyze public holidays for various countries using the Nager.Date API. It supports:

* **Last Three Holidays**: Given a country code, returns the three most recently celebrated holidays (date and localized name).
* **Weekday Holiday Count**: Given a year and a list of country codes, returns the number of public holidays that fall on weekdays for each country, sorted in descending order.
* **Common Holidays**: Given two country codes and a year, returns the list of dates celebrated in both countries, deduplicated with the local name from the first country.

## Solution Design & Assumptions

### Architecture

* **Controller Layer**: Exposes three endpoints under `/holidays`:

    * `GET /last-three?country={code}`
    * `GET /weekday-count?year={year}&countries={code1,code2,...}`
    * `GET /common?year={year}&countryA={code1}&countryB={code2}`

* **Service Layer** (`HolidayService`): Implements business logic, including country code validation, filtering, sorting, and concurrency (via a configurable `Executor`).

* **Client Layer** (`HolidayApiClient` & `NagerDateApiClient`): Wraps calls to the external Nager.Date API using Spring WebClient, with fault tolerance (Resilience4j annotations for retry, rate limiter, fallback) and caching.

* **Configuration** (`CountryCodeLoader`): At startup, fetches and caches valid country codes to validate incoming requests.

* **Domain & DTO**:

    * `Holiday` domain model for API responses.
    * `LastHolidayDTO` and `CountryHolidayCount` for responses to clients.
    * `HolidayMapper` (MapStruct) for converting between domain and DTO.

### Assumptions

* Country codes follow ISO-3166 alpha-2 and are validated at startup.
* The system clock is the source of "today"; cross-year lookup fetches last year's holidays if fewer than three have passed.
* Weekdays are Monday–Friday; Saturday/Sunday are considered weekend.
* Concurrency is optional: default executor can be swapped for single-threaded in tests or a thread pool in production.

## How to Run

1. **Prerequisites**:

    * Java 17+ (tested on 21.0.6)
    * Maven 3.8+

2. **Build**:

   ```bash
   mvn clean package
   ```

3. **Run**:

   ```bash
   mvn spring-boot:run
   ```

   The service starts on port `8080` by default.

4. **Sample Requests**:

   ```bash
   curl "http://localhost:8080/holidays/last-three?country=US"
   curl "http://localhost:8080/holidays/weekday-count?year=2025&countries=US,DE,FR"
   curl "http://localhost:8080/holidays/common?year=2025&countryA=US&countryB=CA"
   ```

## Testing

Run all tests with:

```bash
mvn test
```

## Future Improvements

* **Error Reporting**: Add global exception handling (`@ControllerAdvice`) to unify error responses and include error codes.
* **Metrics & Monitoring**: Integrate Micrometer to track API response times, error rates, and cache hit/miss ratios.
* **Bulk Endpoints**: Support fetching holidays for multiple countries/years in a single request to reduce HTTP overhead.
* **Deployment**: Containerize with Docker, add Kubernetes manifests for cloud deployment and scaling.


