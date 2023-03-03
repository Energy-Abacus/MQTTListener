package org.abacus.script;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class Auth0Token {

    public String getToken() throws IOException, InterruptedException {
        String domain = "https://dev-3adocs3bconafo8d.us.auth0.com/oauth/token";
        String clientId = "UosD9TXobLuQBEuLoKGNBQllPC7qG3Yq";
        String clientSecret = "toedMEKtZ4wzjmCbATOvtEHF8iNeY2ASK1r5LioBsnWuV9yW5H1acxDe8ZetuyED";
        String username = "sultansedad@gmail.com";
        String password = "Password123";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .headers("Content-Type", "application/x-www-form-urlencoded")
                .uri(URI.create(domain))
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=password&username=" + username + "&password=" + password + "&audience=Quarkus-Backend-Abacus&scope=openid contacts&client_id=" + clientId + "&client_secret=" + clientSecret))
                .build();

        HttpResponse<String> response = client.send(request,HttpResponse.BodyHandlers.ofString());
        return response.body();
    }

    public Auth0Token() throws IOException, InterruptedException {
    }
}
