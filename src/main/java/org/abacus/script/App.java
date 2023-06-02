package org.abacus.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

public class App {

    public String dvId;

    public static void main (String[] args) throws IOException, InterruptedException {
        final String publisherId = UUID.randomUUID().toString();
        final String ip = System.getenv("mosquitto_ip");
        final String port = System.getenv("mosquitto_port");
        final String username = System.getenv("mosquitto_user_local");
        final String password = System.getenv("mosquitto_passwd_local");
        JSONtoJava classObject = new JSONtoJava();
        String topic = classObject.getIdAsString();
        Map<String,String> subs = classObject.getSubscriptions();
        Map<String,String> data = new HashMap<>();
        Map<String,String> temp = new HashMap<>();
        int qos = 1;
        /*https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus-backend/api/v1/Measurements*/

        try (IMqttClient client = new MqttClient("tcp://"+ip+":"+port, publisherId)) {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());

            client.setCallback(new MqttCallback() {

                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost: " + cause.getMessage());
                    cause.printStackTrace();
                }

                public void messageArrived(String topic, MqttMessage message) throws MqttException, IOException, InterruptedException {

                    String deviceId;
                    double totalPowerUsed = 0;

                    if(topic.contains("/announce")){

                        deviceId = classObject.getDeviceId(new String(message.getPayload()));
                        classObject.deviceId = deviceId;

                        for (Map.Entry<String,String> item : subs.entrySet()) {
                            subWithId(item.getValue().replace("{id}",deviceId), client);
                        }

                        Auth0Token auth0Token = new Auth0Token();
                        HttpClient client = HttpClient.newHttpClient();
                        ObjectMapper objectMapper = new ObjectMapper();
                        Map<String,String> body = new HashMap<>();
                        body.put("postToken", auth0Token.getToken());
                        body.put("outletIdentifier", classObject.deviceId);
                        String requestBody = objectMapper.writeValueAsString(body);
                        System.out.println(requestBody);

                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create("https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus/elig-update-totalpower/api/v1/measurement/total"))
                                .method("GET", HttpRequest.BodyPublishers.ofString(requestBody))
                                .setHeader("User-Agent", "Java 11 HttpClient Bot")
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        System.out.println(response.statusCode());
                        System.out.println(response.body());
                        totalPowerUsed = Double.parseDouble(response.body());

                    }
                    else{
                        String strTopic = "";
                        for (Map.Entry<String,String> item: subs.entrySet()) {
                            if(item.getValue().replace("{id}",classObject.deviceId).equals(topic)){
                                strTopic = item.getKey();
                            }
                        }

                        data.put(strTopic,new String(message.getPayload()));

                        String key = "";
                        String value = "";

                        for (Map.Entry<String,String> item: data.entrySet()) {
                            if(data.containsKey(item.getKey())){
                                temp.put(item.getKey(),item.getValue());
                                if(item.getKey() == "totalPowerUsed"){
                                    Double result = totalPowerUsed + Double.parseDouble(item.getValue());
                                    key = item.getKey();
                                    value = String.valueOf(result);
                                }
                            }
                        }

                        if(value != "") {
                            data.replace(key, value);
                            key = "";
                            value = "";
                        }

                        if(temp.size() == subs.size()) {
                            String dId = classObject.deviceId;
                            temp.put("outletIdentifier",dId);
                            postToServer(temp);
                            data.clear();
                            temp.clear();
                        }
                    }

                }

                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("deliveryComplete---------" + token.isComplete());
                }

            });

            client.connect(options);
            client.subscribe(topic,qos);

        } catch (MqttException e) {
            System.out.println("Couldnt connect to broker because of: " + e.getMessage());
        }


    }

    static void subWithId(String topic, IMqttClient client) throws MqttException {
        client.subscribe(topic,1);
    }

    static void postToServer(Map<String,String> data) throws IOException, InterruptedException {
        Auth0Token auth0Token = new Auth0Token();
        data.put("timeStamp", String.valueOf(Instant.now().getEpochSecond()));
        data.put("postToken",auth0Token.getToken());
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(data);

        System.out.println(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus/elig-update-totalpower/api/v1/measurement"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }

}