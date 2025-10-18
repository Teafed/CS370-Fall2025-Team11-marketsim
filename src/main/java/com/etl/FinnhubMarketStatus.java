package com.etl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static java.net.http.HttpClient.newHttpClient;

public class FinnhubMarketStatus {

    public static boolean checkStatus() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        String k = System.getenv("FINNHUB_API_KEY");
        if (k == null || k.isBlank()) k = dotenv.get("FINNHUB_API_KEY");

        URI uri = URI.create("https://finnhub.io/api/v1/stock/market-status?exchange=US&token=" + k);

        HttpRequest request = HttpRequest.newBuilder().uri(uri).build();


        HttpResponse<String> response = null;
        try {
            response = newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            JsonObject responseJson = new JsonParser().parse(response.body()).getAsJsonObject();
            String r = responseJson.get("isOpen").getAsString();
            System.out.println(r);
            boolean status = Boolean.parseBoolean(r);
            return status;
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
