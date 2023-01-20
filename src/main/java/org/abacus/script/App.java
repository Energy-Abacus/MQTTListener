package org.abacus.script;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.idm.authorization.AuthorizationRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class App {

    public static void main (String[] args) {
        String publisherId = UUID.randomUUID().toString();
        String ip = System.getenv("mosquitto_ip");
        String port = System.getenv("mosquitto_port");
        String username = System.getenv("mosquitto_user_local");
        String password = System.getenv("mosquitto_passwd_local");
        String deviceName;
        String deviceId;
        JSONtoJava classObject = new JSONtoJava();
        String topic = classObject.getIdAsString();
        Map<String,String> subs = classObject.getSubscriptions();
        Map<String,String> data = new HashMap<>();
        Map<String,String> temp = new HashMap<>();
        int qos = 1;
        IMqttClient client;
        /*https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus-backend/api/v1/Measurements*/


        try {
            client = new MqttClient("tcp://" + ip + ":" + port,publisherId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());

            client.setCallback(new MqttCallback() {

                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost: " + cause.getMessage());
                }

                public void messageArrived(String topic, MqttMessage message) throws MqttException, IOException, InterruptedException {
                    System.out.println("topic: " + topic);
                    System.out.println("Qos: " + message.getQos());
                    System.out.println("message content: " + new String(message.getPayload()));

                    String deviceId;

                    if(new String(message.getPayload()).startsWith("{")) {
                        deviceId = classObject.getDeviceId(new String(message.getPayload()));
                        classObject.deviceId = deviceId;
                        for (Map.Entry<String,String> item : subs.entrySet()) {
                            subWithId(item.getValue().replace("{id}",deviceId), client);
                        }
                    }
                    else{
                        String strTopic = "";
                        for (Map.Entry<String,String> item: subs.entrySet()) {
                            if(item.getValue().replace("{id}",classObject.deviceId).equals(topic)){
                                strTopic = item.getKey();
                            }
                        }

                        data.put(strTopic,new String(message.getPayload()));

                        for (Map.Entry<String,String> item: data.entrySet()) {
                            if(data.containsKey(item.getKey())){
                                temp.put(item.getKey(),item.getValue());
                            }
                        }

                        if(temp.size() == 4) {
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
        KeycloakToken token = new KeycloakToken();
        data.put("AuthToken",token.getRpt());
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBody = objectMapper.writeValueAsString(data);

        System.out.println(requestBody);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://student.cloud.htl-leonding.ac.at/e.gstallnig/abacus-backend/api/v1/Measurements"))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString());

        System.out.println(response.body());
    }
}