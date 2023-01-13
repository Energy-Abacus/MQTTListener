package org.abacus.script;

import org.eclipse.paho.client.mqttv3.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        List<String> subs = classObject.getSubscriptions();
        List<String> subsWithId = new ArrayList<>();
        boolean subscribed = false;
        int qos = 1;
        IMqttClient client;


        try {
            client = new MqttClient("tcp://" + ip + ":" + port,publisherId);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(username);
            options.setPassword(password.toCharArray());

            client.setCallback(new MqttCallback() {

                public void connectionLost(Throwable cause) {
                    System.out.println("connectionLost: " + cause.getMessage());
                }

                public void messageArrived(String topic, MqttMessage message) throws MqttException {
                    System.out.println("topic: " + topic);
                    System.out.println("Qos: " + message.getQos());
                    System.out.println("message content: " + new String(message.getPayload()));

                    String deviceId = "";

                    if(new String(message.getPayload()).startsWith("{")) {
                        deviceId = classObject.getDeviceId(new String(message.getPayload()));
                        System.out.println(deviceId);

                        for (String item: subs) {
                            System.out.println(item);
                            subsWithId.add(item.replace("{id}",deviceId));
                            subWithId(item.replace("{id}",deviceId), client);
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
}