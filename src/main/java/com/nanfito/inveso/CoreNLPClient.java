package com.nanfito.inveso;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CoreNLPClient {
    private HttpClientService httpClientService;

    public CoreNLPClient() {
        this.httpClientService = new HttpClientService();
    }

    public String analyzeText(String text) {
        String serverURL = "http://10.0.0.133:9000"; // Modify with your actual server URL
        JSONObject properties = new JSONObject();
        properties.put("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
        properties.put("outputFormat", "json");

        Map<String, String> params = new HashMap<>();
        params.put("properties", properties.toString());
        params.put("data", text);

        try {
            return httpClientService.sendPostRequest(serverURL, params);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
