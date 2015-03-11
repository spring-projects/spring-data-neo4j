package org.neo4j.cineasts.movieimport;

import org.codehaus.jackson.map.ObjectMapper;

import java.net.URL;
import java.util.Collections;
import java.util.Map;

public class MovieDbApiClient {

    protected final ObjectMapper mapper;
    private final String baseUrl = "https://api.themoviedb.org/3";
    private final String apiKey;

    public MovieDbApiClient(String apiKey) {
        this.apiKey = apiKey;
        mapper = new ObjectMapper();
    }

    public Map getMovie(String id) {
        return loadJsonData(buildMovieUrl(id));
    }

    private Map loadJsonData(String url) {
        try {
            Map value = mapper.readValue(new URL(url), Map.class);
            if (value.isEmpty()) {
                return Collections.singletonMap("not_found", System.currentTimeMillis());
            }
            return value;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get data from " + url, e);
        }
    }

    private String buildMovieUrl(String movieId) {
        return String.format("%s/movie/%s?append_to_response=credits&api_key=%s", baseUrl, movieId, apiKey);
    }

    public Map getPerson(String id) {
        return loadJsonData(buildPersonUrl(id));
    }

    private String buildPersonUrl(String personId) {
        return String.format("%s/person/%s?api_key=%s", baseUrl, personId, apiKey);
    }

    public Map getImageConfig() {
        String url = String.format("%s/configuration?api_key=%s",baseUrl,apiKey);
        return loadJsonData(url);
    }
}
