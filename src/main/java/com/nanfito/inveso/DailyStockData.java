package com.nanfito.inveso;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DailyStockData {
    private String date;
    private double closePrice;
    private String percentChange;
}
