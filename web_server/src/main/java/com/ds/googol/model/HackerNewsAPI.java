package com.ds.googol.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class HackerNewsAPI {
 
    public ArrayList<String> printTopStoryUrls(String input, int numStories) {
        String apiUrl = "https://hacker-news.firebaseio.com/v0/topstories.json";
        String[] terms = input.toLowerCase().split(" ");
        ArrayList<String> allStoryURLs = new ArrayList<>(numStories);
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();
                connection.disconnect();

                // Convertendo a resposta JSON em uma matriz de IDs
                Gson gson = new Gson();
                int[] topStoryIds = gson.fromJson(response.toString(), int[].class);

                // ExecutorService para realizar buscas paralelas
                ExecutorService executor = Executors.newFixedThreadPool(10);
                List<Future<String>> futures = new ArrayList<>(Math.min(numStories, topStoryIds.length));

                // Obtendo os URLs das top stories em paralelo
                for (int i = 0; i < Math.min(numStories, topStoryIds.length); i++) {
                    final int storyId = topStoryIds[i];
                    futures.add(executor.submit(() -> getStoryUrl(storyId, terms)));
                }

                // Aguardando todos os resultados
                for (Future<String> future : futures) {
                    String storyUrl = future.get();
                    if (storyUrl != null && !storyUrl.isEmpty()) { // Verifica se o URL não é nulo nem vazio
                        allStoryURLs.add(storyUrl);
                    }
                }

                executor.shutdown();
                return allStoryURLs;
            } else {
                System.out.println("Erro ao fazer a requisição. Código de resposta: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            System.out.println("Erro ao obter as top stories: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String getStoryUrl(int id, String[] terms) throws Exception {
        String apiUrl = "https://hacker-news.firebaseio.com/v0/item/" + id + ".json";
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            connection.disconnect();

            // Convertendo a resposta JSON para obter o URL da história
            Gson gson = new Gson();
            JsonObject storyObject = gson.fromJson(response.toString(), JsonObject.class);
            boolean containsAllTerms = true;
            for(String term : terms){
                if(storyObject.get("type").getAsString().equals("story")) {
                    String title = storyObject.has("title") ? storyObject.get("title").getAsString().toLowerCase(): "";
                    String text = storyObject.has("text") ? storyObject.get("text").getAsString().toLowerCase() : "";
                    if (!title.contains(term) && !text.contains(term)) {
                        containsAllTerms = false;
                        break;
                    }
                } else {
                    containsAllTerms = false;
                    break;
                }
            }
            if (containsAllTerms && storyObject.get("type").getAsString().equals("story") && storyObject.has("url") && storyObject.has("title")) {
                String storyUrl = storyObject.get("url").getAsString();
                System.out.println("Story URL: " + storyUrl);
                System.out.println("Story Title: " + storyObject.get("title").getAsString());
                if (storyObject.has("text")) {
                    System.out.println("Story Text: " + storyObject.get("text").getAsString());
                }
                return storyUrl;
            }
            return "";
        } else {
            throw new Exception("Erro ao obter a URL da história. Código de resposta: " + responseCode);
        }
    }
}
