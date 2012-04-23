package org.neo4j.cineasts.movieimport;


import org.codehaus.jackson.map.ObjectMapper;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MovieDbApiClient {

    private final String baseUrl = "http://api.themoviedb.org/";
    private final String apiKey;
    protected final ObjectMapper mapper;

    public MovieDbApiClient(String apiKey) {
        this.apiKey = apiKey;
        mapper = new ObjectMapper();
    }

    public Map getMovie(String id) {
        return loadJsonData(id, buildMovieUrl(id));
    }

    private Map loadJsonData(String id, String url) {
        try {
            List value = mapper.readValue(new URL(url), List.class);
            if (value.isEmpty() || value.get(0).equals("Nothing found.")) return Collections.singletonMap("not_found",System.currentTimeMillis());
            return (Map) value.get(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get data from " + url, e);
        }
    }

    private String buildMovieUrl(String movieId) {
        return String.format("%s2.1/Movie.getInfo/en/json/%s/%s", baseUrl, apiKey, movieId);
    }

    public Map getPerson(String id) {
        return loadJsonData(id, buildPersonUrl(id));
    }

    private String buildPersonUrl(String personId) {
        return String.format("%s2.1/Person.getInfo/en/json/%s/%s", baseUrl, apiKey, personId);
    }
}
