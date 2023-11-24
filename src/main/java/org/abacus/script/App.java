package org.abacus.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class App {

    public String dvId;

    public static void main (String[] args) {
        final String publisherId = UUID.randomUUID().toString();
        final String ip = System.getenv("mosquitto_ip");
        final String port = System.getenv("mosquitto_port");
        final String username = System.getenv("mosquitto_user_local");
        final String password = System.getenv("mosquitto_passwd_local");

        List<PlugData> plugDataList = new ArrayList<>();

        try (IMqttAsyncClient client = new MqttAsyncClient("tcp://"+ip+":"+port, publisherId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());

            client.setCallback(new MqttCallback() {

                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost: " + cause.getMessage());
                    cause.printStackTrace();
                }

                public void messageArrived(String topic, MqttMessage message) {

                    final PlugData plugData = plugDataList.stream()
                            .filter(p -> p.getSubs().containsKey(topic))
                            .findFirst()
                            .orElse(null);

                    if (plugData == null) {
                        System.out.println("No plug found for topic: " + topic);
                        return;
                    }

                    final Topic topicObj = plugData.getSubs().get(topic);
                    final String payload = new String(message.getPayload());

                    switch (topicObj.getName()) {
                        case "wattPower" -> plugData.setWattPower(Double.parseDouble(payload));
                        case "temperature" -> plugData.setTemperature(Double.parseDouble(payload));
                        case "energy" -> plugData.setTotalPowerUsed(Double.parseDouble(payload));
                    }

                    try {
                        plugData.postAndClearIfReady();
                    } catch (IOException e) {
                        System.out.println("Error while posting data: " + e.getMessage());
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

            });

            IMqttToken token = client.connect(options);
            token.waitForCompletion();

            HttpClient httpClient = HttpClient.newHttpClient();
            Auth0Token authToken = new Auth0Token();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus/main/api/v1/outlet/by-hub?postToken=" + authToken.getToken()))
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .setHeader("User-Agent", "Java 11 HttpClient Bot")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray jsonArray = (JSONArray)(new JSONParser()).parse(response.body());

            for (Object id : jsonArray) {
                PlugData plugData = new PlugData((String)id);
                plugDataList.add(plugData);
                for (Topic topic : plugData.getSubs().values()) {
                    subWithId(topic.getTopic(), client);
                }
            }
        } catch (MqttException e) {
            System.out.println("Couldnt connect to broker because of: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException | InterruptedException | ParseException e) {
            System.out.println("Couldnt get plugs from api because: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    static void subWithId(String topic, IMqttAsyncClient client) throws MqttException {
        client.subscribe(topic,1);
    }
}