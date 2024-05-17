package com.ds.googol.model;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.google.gson.*;

public class PokemonAPI {

    private HashMap<String, String> pokemonNamesAndURLs = new HashMap<>();
    // Will hold the name and urls of all the pokemons existing at the time
    
    public int getPokemonCount() throws Exception {
        // API URL to get the total number of Pokémon
        String apiUrl = "https://pokeapi.co/api/v2/pokemon?limit=1";
        try{
            // Connecting to the API
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // For this API, it is necessary to set a User-Agent, it can be anything
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Checking if the connection was successful
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                // Reading the API response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                // Reading each line of the response and adding to the StringBuilder
                String inputLine;
                while((inputLine = in.readLine()) != null){
                    response.append(inputLine);
                }
                // Closing the BufferedReader and disconnecting the connection
                in.close();
                connection.disconnect();

                // Converting the JSON response to get the Pokémon count
                Gson gson = new Gson();
                JsonObject pokemonCount = gson.fromJson(response.toString(), JsonObject.class);
                //System.out.println("Número de Pokémon: " + pokemonCount.get("count").getAsInt());
                // Returning the Pokémon count
                return pokemonCount.get("count").getAsInt();
            }
        } catch(Exception e) {
            System.out.println("Erro ao obter o número de Pokémon: " + e.getMessage());
        }
        return 0;
    }

    public void updatePokemonsMap() throws Exception {
        // Getting the total number of Pokémon
        int count = getPokemonCount();
        // API URL to get the Pokémon names in the range of the total number of Pokémon
        String apiUrl = "https://pokeapi.co/api/v2/pokemon?loffset="+count+"&limit="+count;

        try {
            // Connecting to the API
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // For this API, it is necessary to set a User-Agent, it can be anything
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Checking if the connection was successful
            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                // Reading the API response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                // Reading each line of the response and adding to the StringBuilder
                while((inputLine = in.readLine()) != null){
                    response.append(inputLine);
                }
                // Closing the BufferedReader and disconnecting the connection
                in.close();
                connection.disconnect();

                // Creating a HashMap to hold the Pokémon names and URLs
                Gson gson = new Gson();
                JsonObject pokemonsData = gson.fromJson(response.toString(), JsonObject.class);
               
                // Creating a HashMap to hold the Pokémon names and URLs
                JsonArray results = pokemonsData.getAsJsonArray("results");
                for (JsonElement pokemon : results) {
                    String pokeName = pokemon.getAsJsonObject().get("name").getAsString();
                    String pokeURL = pokemon.getAsJsonObject().get("url").getAsString();
                    pokemonNamesAndURLs.put(pokeName, pokeURL);
                    //System.out.println(pokeName);
                }
             }
             //return pokemonNamesAndURLs;

        } catch (Exception e) {
            System.out.println("Erro ao obter os Pokémon: " + e.getMessage());
            e.printStackTrace();
            //return null;
        }
    }
    public ArrayList<String> getPokeInfo(String pokeName) throws Exception{
        // Obtain the URL of the Pokémon
        String apiUrl = pokemonNamesAndURLs.get(pokeName);
        ArrayList<String> pokeInfoArrayList = new ArrayList<>();
        ArrayList<String> allTypesUrl = new ArrayList<>();
        try{
            // Connecting to the API
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // For this API, it is necessary to set a User-Agent, it can be anything
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            // Checking if the connection was successful
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Reading the API response
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String inputLine;
                // Reading each line of the response and adding to the StringBuilder
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);;
                }
                // Closing the BufferedReader and disconnecting the connection
                in.close();
                connection.disconnect();

                // Converting the JSON response to get the Pokémon information
                Gson gson = new Gson();
                JsonObject pokemonData = gson.fromJson(response.toString(), JsonObject.class);
                // Verifying if the Pokémon name exists
                if(pokemonData.has("name")){
                    String pokemonName = pokemonData.get("name").getAsString().replace("-", " ").replaceFirst(pokemonData.get("name").getAsString().substring(0, 1), pokemonData.get("name").getAsString().substring(0, 1).toUpperCase());
                    Double pokemonHeight = pokemonData.get("height").getAsDouble()/10;
                    Double pokemonWeight = pokemonData.get("weight").getAsDouble()/10;
                    String pokemonBaseExperience = pokemonData.get("base_experience").getAsString();
                    pokeInfoArrayList.add("Name: " + pokemonName+ ".");
                    pokeInfoArrayList.add("Height: " + pokemonHeight + " m.");
                    pokeInfoArrayList.add("Weight: " + pokemonWeight + " kg.");
                    pokeInfoArrayList.add("Base Experience: " + pokemonBaseExperience + " XP.");
                    
                    ArrayList<String> pokeTypes = new ArrayList<>();
                    for(JsonElement type : pokemonData.getAsJsonArray("types")){
                        String typeName = type.getAsJsonObject().get("type").getAsJsonObject().get("name").getAsString();
                        String typeURL = type.getAsJsonObject().get("type").getAsJsonObject().get("url").getAsString();
                        allTypesUrl.add(typeURL);
                        pokeTypes.add(typeName);
                    }
                    pokeInfoArrayList.add("Types: " + String.join(", ", pokeTypes) + ".");
                    getTypesInfo(allTypesUrl, pokeInfoArrayList);
                    if(pokemonData.has("sprites")){
                        if(pokemonData.getAsJsonObject("sprites").get("front_default").isJsonNull()){
                            pokeInfoArrayList.add("");
                        }else{
                            String pokemonSprite = pokemonData.getAsJsonObject("sprites").get("front_default").getAsString();
                            pokeInfoArrayList.add(pokemonSprite);
                        }
                    }
                    return pokeInfoArrayList;
                }
            }
            pokeInfoArrayList.add("Pokémon not found.");
            return pokeInfoArrayList;
        } catch (Exception e) {
            pokeInfoArrayList.add("Pokémon not found.");
            return pokeInfoArrayList;
        }
    }
    public ArrayList<String> getRandomPokemon() throws Exception{
        try {
        // Random object to get a random Pokémon
        // Getting all the Pokémon names
        ArrayList<String> pokemons = new ArrayList<String>(pokemonNamesAndURLs.keySet());
        // Getting a random Pokémon name
        Random random = new Random();
        int randomIndex = random.nextInt(pokemons.size());
        String randomPokemon = pokemons.get(randomIndex);
        // Returning the Pokémon information
        return getPokeInfo(randomPokemon);
        } catch (Exception e) {
            System.out.println("Erro ao obter o Pokémon: " + e.getMessage());
            //e.printStackTrace();
        }
        ArrayList<String> error = new ArrayList<>();
        error.add("Erro ao obter o Pokémon");
        return error;
    }
    private static ArrayList<String> getTypesInfo(ArrayList<String> typesURL, ArrayList<String> pokeInfoArrayList) {
        HashMap<String, List<String>> damageRelationsMap = new HashMap<>();
        for (int i = 0; i < typesURL.size(); i++){
            try {
                URL url = new URL(typesURL.get(i));
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/5.0");

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);;
                    }
                    in.close();
                    connection.disconnect();
                    List<String> noDamageFrom = new ArrayList<>();
                    List<String> noDamageTo = new ArrayList<>();
                    List<String> doubleDamageTo = new ArrayList<>();
                    List<String> halfDamageTo = new ArrayList<>();
                    List<String> doubleDamageFrom = new ArrayList<>();
                    List<String> halfDamageFrom = new ArrayList<>();
                    Gson gson = new Gson();
                    JsonObject typeData = gson.fromJson(response.toString(), JsonObject.class);
                    for(JsonElement damageRelations : typeData.getAsJsonObject("damage_relations").getAsJsonArray("double_damage_to")){
                        doubleDamageTo.add(damageRelations.getAsJsonObject().get("name").getAsString());
                    }
                    for(JsonElement damageRelations : typeData.getAsJsonObject("damage_relations").getAsJsonArray("half_damage_to")){
                        halfDamageTo.add(damageRelations.getAsJsonObject().get("name").getAsString());
                    }
                    for(JsonElement damageRelations : typeData.getAsJsonObject("damage_relations").getAsJsonArray("no_damage_to")){
                        noDamageTo.add(damageRelations.getAsJsonObject().get("name").getAsString());
                    }
                    for(JsonElement damageRelations : typeData.getAsJsonObject("damage_relations").getAsJsonArray("double_damage_from")){
                        doubleDamageFrom.add(damageRelations.getAsJsonObject().get("name").getAsString());
                    }
                    for(JsonElement damageRelations : typeData.getAsJsonObject("damage_relations").getAsJsonArray("half_damage_from")){
                        halfDamageFrom.add(damageRelations.getAsJsonObject().get("name").getAsString());
                       
                    }
                    for(JsonElement damageRelations : typeData.getAsJsonObject("damage_relations").getAsJsonArray("no_damage_from")){
                        noDamageFrom.add(damageRelations.getAsJsonObject().get("name").getAsString());
                    }
                    damageRelationsMap.put("doubleDamageFrom",doubleDamageFrom);
                    damageRelationsMap.put("noDamageTo",noDamageTo);
                    damageRelationsMap.put("halfDamageTo",halfDamageTo);
                    damageRelationsMap.put("doubleDamageTo",doubleDamageTo);
                    damageRelationsMap.put("halfDamageFrom",halfDamageFrom);
                    damageRelationsMap.put("noDamageFrom",noDamageFrom);
                    return handleDamageRelations(damageRelationsMap, pokeInfoArrayList);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    private static ArrayList<String> handleDamageRelations(HashMap<String,List<String>> damageRelationsMap, ArrayList<String> pokeInfoArrayList){
        if(damageRelationsMap.containsKey("doubleDamageTo")){
            if (damageRelationsMap.get("doubleDamageTo").isEmpty()){
                pokeInfoArrayList.add("Double damage to: none. ");
            }else{
            pokeInfoArrayList.add("Double damage to: " + String.join(", ", damageRelationsMap.get("doubleDamageTo")) + ".");
            }
        }
        if(damageRelationsMap.containsKey("halfDamageTo")){
            if (damageRelationsMap.get("halfDamageTo").isEmpty()){
                pokeInfoArrayList.add("Half damage to: none. ");
            }else{
                pokeInfoArrayList.add("Half damage to: " + String.join(", ", damageRelationsMap.get("halfDamageTo")) + ".");
            }
        }
        if(damageRelationsMap.containsKey("noDamageTo")){
            if (damageRelationsMap.get("noDamageTo").isEmpty()){
                pokeInfoArrayList.add("No damage to: none. ");
            }else{
            pokeInfoArrayList.add("No damage to: " + String.join(", ", damageRelationsMap.get("noDamageTo")) + ".");
            }
        }
        if(damageRelationsMap.containsKey("doubleDamageFrom")){
            if (damageRelationsMap.get("doubleDamageFrom").isEmpty()){
                pokeInfoArrayList.add("Double damage from: none. ");
            }else{
                pokeInfoArrayList.add("Double damage from: " + String.join(", ", damageRelationsMap.get("doubleDamageFrom")) + ".");
            }
        }
        if(damageRelationsMap.containsKey("halfDamageFrom")){
            if (damageRelationsMap.get("halfDamageFrom").isEmpty()){
                pokeInfoArrayList.add("Half damage from: none. ");
            }else{
                pokeInfoArrayList.add("Half damage from: " + String.join(", ", damageRelationsMap.get("halfDamageFrom")) + ".");
            }
        }
        if(damageRelationsMap.containsKey("noDamageFrom")){
            if (damageRelationsMap.get("noDamageFrom").isEmpty()){
                pokeInfoArrayList.add("No damage from: none. ");
            }else{
                pokeInfoArrayList.add("No damage from: " + String.join(", ", damageRelationsMap.get("noDamageFrom")) + ".");
            }
        }
        
        return pokeInfoArrayList;
    }
}