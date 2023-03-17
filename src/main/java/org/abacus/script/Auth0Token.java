package org.abacus.script;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;


public class Auth0Token {

    public String getToken() throws IOException, InterruptedException {
        return System.getenv("post_token");
    }

    public Auth0Token() throws IOException, InterruptedException {
    }
}
