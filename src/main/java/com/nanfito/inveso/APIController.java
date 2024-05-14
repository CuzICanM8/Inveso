package com.nanfito.inveso;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Component
public class APIController {
    private final RestTemplate restTemplate;
    private final String apiKey = "0nwrcFPstrH7N86FHnvCh1KeXqb5XCey";
    private PolygonApiResponse cachedResponse;
    private LocalDate lastFetchDate;
    private String lastFetchedTicker;  // Store the last fetched ticker


    public APIController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private void fetchDataIfStale(String ticker) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        // Fetch data only if it hasn't been fetched today or if there's no cached response or if the ticker has changed
        if (cachedResponse == null || lastFetchDate == null || !lastFetchDate.equals(today) || !ticker.equals(lastFetchedTicker)) {
            String from = today.minusDays(365).format(formatter);  // earliest data point
            String to = today.format(formatter);  // most recent data point

            String url = String.format("https://api.polygon.io/v2/aggs/ticker/%s/range/1/day/%s/%s?apiKey=%s",
                    ticker, from, to, apiKey);

            System.out.println("Fetching new data from API: " + url);
            cachedResponse = restTemplate.getForObject(url, PolygonApiResponse.class);
            lastFetchDate = today;  // Update the last fetch date
            lastFetchedTicker = ticker;  // Update the last fetched ticker
            System.out.println("Data cached.");
        }
    }

    public PolygonApiResponse getStockGraph(String ticker) {
        fetchDataIfStale(ticker);
        // Assuming the graph requires a longer range, say the last 90 days, already covered by fetchDataIfStale
        return cachedResponse; // Directly return the data for graph plotting
    }

    public PolygonApiResponse getStockTable(String ticker) {
        fetchDataIfStale(ticker);
        LocalDate to = LocalDate.now();
        LocalDate from = LocalDate.now().minusDays(31);

        // Filter the cached data to only include the last x days
        PolygonApiResponse filteredResponse = new PolygonApiResponse();
        if (cachedResponse != null && cachedResponse.getResults() != null) {
            filteredResponse.setResults(
                    cachedResponse.getResults().stream()
                            .filter(result -> {
                                LocalDate date = result.getDate(); // Directly using LocalDate from Result
                                return !date.isBefore(from) && !date.isAfter(to);
                            })
                            .collect(Collectors.toList())
            );
        }
        return filteredResponse;
    }
}
