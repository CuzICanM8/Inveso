package com.nanfito.inveso;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Data
public class PolygonApiResponse {
    private String status;
    private int queryCount;
    private int resultsCount;
    private boolean adjusted;
    private String ticker;
    private List<Result> results;

    @Data
    public static class Result {
        private double v;  // Volume
        private double vw; // Volume-weighted average price
        private double o;  // Open
        private double c;  // Close
        private double h;  // High
        private double l;  // Low
        private long t;    // Timestamp
        private int n;     // Number of transactions

        public LocalDate getDate() {
            return Instant.ofEpochMilli(t).atZone(ZoneId.systemDefault()).toLocalDate();
        }
    }
}
