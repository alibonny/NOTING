package com.google.gwt.sample.stockwatcher.server;

import com.google.gwt.sample.stockwatcher.shared.DelistedException;
import com.google.gwt.sample.stockwatcher.shared.StockPrice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StockPriceServiceTest {

    StockPriceServiceImpl service = new StockPriceServiceImpl();

    @BeforeEach
    public void cleanEnvironment() {

    }

    @DisplayName("Test testing is working")
    @Test
    public void testGetPrices() {
        String[] symbols = new String[] {"FTSEMIB", "APL", "AMD", "INTC"};

        try {
            StockPrice[] stockPrices = service.getPrices(symbols);
            assertEquals(stockPrices.length, symbols.length);
        } catch (DelistedException e) {
            // Do nothing
        }
    }
}
