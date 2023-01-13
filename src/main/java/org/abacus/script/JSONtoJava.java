package org.abacus.script;

import java.io.*;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class JSONtoJava {

    String id;
    List<String> subs = new ArrayList<>();

    {
        JSONParser parser = new JSONParser();
        try {
            Object object;
            object = parser.parse(new FileReader("C:\\4BHIF\\SYP\\SubscribeScript\\src\\main\\java\\org\\abacus\\script\\Subscriptions.json"));
            JSONObject obj = (JSONObject)object;
            JSONObject data = (JSONObject)obj.get("data");
            JSONArray devices = (JSONArray)data.get("devices");
            JSONObject idObject = (JSONObject)devices.get(0);
            id = (String)idObject.get("id");
            JSONObject interfaces = (JSONObject)idObject.get("interfaces");

            subs.add((String)interfaces.get("status"));
            subs.add((String)interfaces.get("wattPower"));
            subs.add((String)interfaces.get("wattMinutePower"));
            subs.add((String)interfaces.get("temperature"));
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    public String getIdAsString(){
        return id;
    }

    public List<String> getSubscriptions(){
        return subs;
    }

    public String getDeviceId(String jsonStr){
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
