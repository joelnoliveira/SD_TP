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

/**
 * The HackerNewsAPI class provides methods to get the top stories from the Hacker News API.
 */
public class HackerNewsAPI {
    
    /**
     * Prints the URLs of the top stories that contain all the terms in the input.
     * 
     * @param input the input string containing the terms to search for
     * @param numStories the number of top stories to print
     * @return the list of URLs of the top stories
     */
    public ArrayList<String> printTopStoryUrls(String input, int numStories) {
        // API URL to get the top stories
        String apiUrl = "https://hacker-news.firebaseio.com/v0/topstories.json";
        // Splitting the input into terms
        String[] terms = input.toLowerCase().split(" ");
        // List to store all the story URLs
        ArrayList<String> allStoryURLs = new ArrayList<>(numStories);
        try {
            // Opening a connection to the API URL
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Verifying the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Reading the response line by line and storing in a StringBuilder
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Closing the BufferedReader and disconnecting the connection
                in.close();
                connection.disconnect();

                // Converting the JSON response to get the top story IDs
                Gson gson = new Gson();
                int[] topStoryIds = gson.fromJson(response.toString(), int[].class);

                // Creating a fixed thread pool with 10 threads
                ExecutorService executor = Executors.newFixedThreadPool(10);
                List<Future<String>> futures = new ArrayList<>(Math.min(numStories, topStoryIds.length));

                // Getting the URL of the top stories
                for (int i = 0; i < Math.min(numStories, topStoryIds.length); i++) {
                    final int storyId = topStoryIds[i];
                    futures.add(executor.submit(() -> getStoryUrl(storyId, terms)));
                }

                // Adding the URLs to the list
                for (Future<String> future : futures) {
                    String storyUrl = future.get();
                    if (storyUrl != null && !storyUrl.isEmpty()) { // Verifying if the URL is not empty
                        allStoryURLs.add(storyUrl);
                    }
                }
                
                // Shutting down the executor
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

    /**
     * Gets the URL of the story with the given ID and containing all the terms.
     * 
     * @param id the ID of the story
     * @param terms the terms to search for
     * @return the URL of the story
     */
    private String getStoryUrl(int id, String[] terms) {
        // API URL to get the story details
        String apiUrl = "https://hacker-news.firebaseio.com/v0/item/" + id + ".json";

        try{
            // Opening a connection to the API URL
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Verifying the response code
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Reading the response line by line and storing in a StringBuilder
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                // Closing the BufferedReader and disconnecting the connection
                in.close();
                connection.disconnect();

                // Converting the JSON response to get the story details
                Gson gson = new Gson();
                JsonObject storyObject = gson.fromJson(response.toString(), JsonObject.class);
                boolean containsAllTerms = true;
                // Verifying if the story contains all the terms
                for(String term : terms){
                    // Verifying if the story is a story and contains the term in the title or text
                    if(storyObject.get("type").getAsString().equals("story")) {
                        String title = storyObject.has("title") ? storyObject.get("title").getAsString().toLowerCase(): "";
                        String text = storyObject.has("text") ? storyObject.get("text").getAsString().toLowerCase() : "";
                        // If the story does not contain the term in the title or text, it does not contain all the terms
                        if (!title.contains(term) && !text.contains(term)) {
                            containsAllTerms = false;
                            break;
                        }
                    // If the story is not a story, it does not contain all the terms
                    } else {
                        containsAllTerms = false;
                        break;
                    }
                }
                // If the story contains all the terms, it prints the URL, title and text of the story
                if (containsAllTerms && storyObject.get("type").getAsString().equals("story") && storyObject.has("url") && storyObject.has("title")) {
                    String storyUrl = storyObject.get("url").getAsString();
                    System.out.println("Story URL: " + storyUrl);
                    System.out.println("Story Title: " + storyObject.get("title").getAsString());
                    // If the story has text, it prints the text of the story
                    if (storyObject.has("text")) {
                        System.out.println("Story Text: " + storyObject.get("text").getAsString());
                    }
                    return storyUrl;
                }
                return "";
            } else {
                System.out.println("Erro ao fazer a requisição. Código de resposta: " + responseCode);
            }
        } catch (Exception e) {
            System.out.println("Erro ao obter a história: " + e.getMessage());
            e.printStackTrace();
        }
        return "";
    }
}
