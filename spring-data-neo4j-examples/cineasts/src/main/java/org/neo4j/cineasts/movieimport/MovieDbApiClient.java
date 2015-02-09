package org.neo4j.cineasts.movieimport;

import org.codehaus.jackson.map.ObjectMapper;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MovieDbApiClient {

    private final String baseUrl = "http://api.themoviedb.org";
    private final String apiKey;
    protected final ObjectMapper mapper;
    private final Map config;
    private final String profileFormat;
    private final String posterFormat;


    public MovieDbApiClient(String apiKey) {
        this.apiKey = apiKey;
        mapper = new ObjectMapper();
        config = loadJsonData(buildConfigUrl());
        Map images = (Map) config.get("images");
        profileFormat=String.format("%s%s/%%s", images.get("base_url"),((List)images.get("profile_sizes")).get(0));
        posterFormat=String.format("%s%s/%%s", images.get("base_url"),((List)images.get("poster_sizes")).get(3));

    }

    public Map getMovie(String id) {
        return loadJsonData(buildMovieUrl(id));
    }

    private Map loadJsonData(String url) {
        try {
            Object value = mapper.readValue(new URL(url), Object.class);
            Map map = null;
            if (value instanceof List) {
                List list= (List) value;
                if (list.isEmpty() || list.get(0).equals("Nothing found."))
                    return notFound();
                map = (Map)list.get(0);
            }
            if (value instanceof Map) {
                map = (Map)value;
            }
            if (map == null ) {
                return notFound();
            }
            if (map.containsKey("status_code")) {
                map.putAll(notFound());
            }
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get data from " + url, e);
        }
    }

    public Map getConfig() {
        return config;
    }

    public String getProfileFormat() {
        return profileFormat;
    }

    public String getPosterFormat() {
        return posterFormat;
    }

    private Map<String, Long> notFound() {
        return Collections.singletonMap("not_found", System.currentTimeMillis());
    }

    private String buildMovieUrl(String movieId) {
        return String.format("%s/3/movie/%s?api_key=%s&append_to_response=credits,trailers", baseUrl, movieId, apiKey);
    }
    private String buildConfigUrl() {
        return String.format("%s/3/configuration?api_key=%s", baseUrl, apiKey);
    }

    public Map getPerson(String id) {
        return loadJsonData(buildPersonUrl(id));
    }

    private String buildPersonUrl(String personId) {
        return String.format("%s/3/person/%s?api_key=%s", baseUrl,  personId, apiKey);
    }
}
