package com.etl;

import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FinnhubProfileClientTest {

    @Test
    void parsesCompanyProfile() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResp = (HttpResponse<String>) mock(HttpResponse.class);

        String json = "{" +
                "\"country\":\"US\"," +
                "\"currency\":\"USD\"," +
                "\"exchange\":\"NASDAQ\"," +
                "\"finnhubIndustry\":\"Technology\"," +
                "\"ipo\":\"1980-12-12\"," +
                "\"marketCapitalization\":2345678.9," +
                "\"name\":\"Test Corp\"," +
                "\"phone\":\"+1-555-0100\"," +
                "\"shareOutstanding\":1234567.0," +
                "\"ticker\":\"TC\"," +
                "\"weburl\":\"https://test.example.com\", " +
                "\"logo\":\"https://test.example.com/logo.png\"" +
                "}";

        when(mockResp.statusCode()).thenReturn(200);
        when(mockResp.body()).thenReturn(json);
    when(mockHttp.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResp);

        FinnhubProfileClient client = new FinnhubProfileClient(mockHttp, "test-key", "https://finnhub.io/api/v1");
        FinnhubProfileClient.CompanyProfile p = client.getCompanyProfile("TC");

        assertEquals("US", p.country());
        assertEquals("USD", p.currency());
        assertEquals("NASDAQ", p.exchange());
        assertEquals("Technology", p.finnhubIndustry());
        assertEquals("1980-12-12", p.ipo());
        assertEquals(2345678.9, p.marketCapitalization());
        assertEquals("Test Corp", p.name());
        assertEquals("+1-555-0100", p.phone());
        assertEquals(1234567.0, p.shareOutstanding());
        assertEquals("TC", p.ticker());
        assertEquals("https://test.example.com", p.weburl());
        assertEquals("https://test.example.com/logo.png", p.logo());
    }

    @Test
    void non200Throws() throws Exception {
        HttpClient mockHttp = mock(HttpClient.class);
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResp = (HttpResponse<String>) mock(HttpResponse.class);

        when(mockResp.statusCode()).thenReturn(403);
        when(mockResp.body()).thenReturn("{\"error\":\"forbidden\"}");
    when(mockHttp.send(any(), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(mockResp);

        FinnhubProfileClient client = new FinnhubProfileClient(mockHttp, "test-key", "https://finnhub.io/api/v1");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> client.getCompanyProfile("TC"));
        assertTrue(ex.getMessage().contains("profile request failed"));
    }
}
