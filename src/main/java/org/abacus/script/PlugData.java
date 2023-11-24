package org.abacus.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PlugData {
    private String deviceId;
    private double startPowerUsed;
    private double totalPowerUsed;
    private double wattPower;
    private double temperature;

    private String announce;
    private Map<String,Topic> subs = new HashMap<>();

    public void setTotalPowerUsed(double totalPowerUsed) {
        this.totalPowerUsed = totalPowerUsed;
    }

    public void setWattPower(double wattPower) {
        this.wattPower = wattPower;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public double getTotalPowerUsed() {
        return totalPowerUsed;
    }

    public double getWattPower() {
        return wattPower;
    }

    public double getTemperature() {
        return temperature;
    }

    public String getAnnounce() {
        return announce;
    }

    public Map<String, Topic> getSubs() {
        return subs;
    }

    public PlugData(String deviceId) {
        this.deviceId = deviceId;

        try {
            this.startPowerUsed = getStartPowerUsed();
        } catch (IOException e) {
            System.out.println("Error while getting start power used from API");
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }

        System.out.println("Start power" + this.startPowerUsed);

        JSONParser parser = new JSONParser();
        try {
            Object object;
            object = parser.parse(new FileReader("src/main/java/org/abacus/script/Subscriptions.json"));
            JSONObject obj = (JSONObject)object;
            JSONObject data = (JSONObject)obj.get("data");
            JSONArray devices = (JSONArray)data.get("devices");
            JSONObject idObject = (JSONObject)devices.get(0);
            announce = (String)idObject.get("id");
            String topicSeparator = (String)idObject.get("topicSeparator");
            JSONObject interfaces = (JSONObject)idObject.get("interfaces");

            interfaces.forEach((name, topic) -> {
                topic = ((String)topic).replace("{id}", deviceId);
                subs.put((String) topic, new Topic((String) topic, (String) name, topicSeparator));
            });
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public void postAndClearIfReady() throws IOException, InterruptedException {
        if (Double.isNaN(this.totalPowerUsed) || Double.isNaN(this.wattPower) || Double.isNaN(this.temperature)) {
            return;
        }

        Auth0Token auth0Token = new Auth0Token();

        Map<String,String> data = new HashMap<>();

        data.put("timeStamp", String.valueOf(Instant.now().getEpochSecond()));
        data.put("postToken", auth0Token.getToken());
        data.put("outletIdentifier", this.deviceId);
        data.put("totalPowerUsed", String.valueOf(this.startPowerUsed + this.totalPowerUsed));
        data.put("wattPower", String.valueOf(this.wattPower));
        data.put("temperature", String.valueOf(this.temperature));

        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(data);

        System.out.println("Debug body: " + requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus/main/api/v1/measurement"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        this.totalPowerUsed = Double.NaN;
        this.wattPower = Double.NaN;
        this.temperature = Double.NaN;
    }

    private double getStartPowerUsed() throws IOException, InterruptedException {
        Auth0Token auth0Token = new Auth0Token();
        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper objectMapper = new ObjectMapper();

        Map<String,String> body = new HashMap<>();
        body.put("postToken", auth0Token.getToken());
        body.put("outletIdentifier", this.deviceId);
        String requestBody = objectMapper.writeValueAsString(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus/main/api/v1/measurement/total-power-plug"))
                .method("GET", HttpRequest.BodyPublishers.ofString(requestBody))
                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        return Double.parseDouble(response.body());
    }

    public static String getDeviceId(String jsonStr){
        JSONParser parser = new JSONParser();
        String result = "";
        try {
            JSONObject jsonObject = (JSONObject)parser.parse(jsonStr);
            result = (String)jsonObject.get("id");
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return result;
    }
}
