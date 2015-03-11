package org.neo4j.cineasts.movieimport;

import org.neo4j.cineasts.domain.Movie;
import org.neo4j.cineasts.domain.Person;
import org.neo4j.cineasts.domain.Roles;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class MovieDbJsonMapper {

    public void mapToMovie(Map data, Movie movie, String baseImageUrl) {
        try {
            movie.setTitle((String) data.get("title"));
            movie.setLanguage((String) data.get("original_language"));
            movie.setImdbId((String) data.get("imdb_id"));
            movie.setTagline((String) data.get("tagline"));
            movie.setDescription(limit((String) data.get("overview"), 500));
            movie.setReleaseDate(toDate(data, "release_date", "yyyy-MM-dd"));
            movie.setRuntime((Integer) data.get("runtime"));
            movie.setHomepage((String) data.get("homepage"));
            movie.setTrailer((String) data.get("trailer")); //TODO missing
            movie.setGenre(extractFirst(data, "genres", "name"));
            movie.setStudio(extractFirst(data, "production_companies", "name"));
            movie.setImageUrl(baseImageUrl + data.get("poster_path"));
        } catch (Exception e) {
            throw new MovieDbException("Failed to map json for movie", e);
        }
    }

    private String selectImageUrl(List<Map> data, final String type, final String size) {
        if (data == null) {
            return null;
        }
        for (Map entry : data) {
            Map image = (Map) entry.get("image");
            if (image.get("type").equals(type) && image.get("size").equals(size)) {
                return (String) image.get("url");
            }
        }
        return null;
    }


    private String extractFirst(Map data, String field, String property) {
        List<Map> inner = (List<Map>) data.get(field);
        if (inner == null || inner.isEmpty()) {
            return null;
        }
        return (String) inner.get(0).get(property);
    }

    private Date toDate(Map data, String field, final String pattern) throws ParseException {
        try {
            String dateString = (String) data.get(field);
            if (dateString == null || dateString.isEmpty()) {
                return null;
            }
            return new SimpleDateFormat(pattern).parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }


    public void mapToPerson(Map data, Person person) {
        try {
            person.setName((String) data.get("name"));
            person.setBirthday(toDate(data, "birthday", "yyyy-MM-dd"));
            person.setBirthplace((String) data.get("birthplace"));
            String biography = (String) data.get("biography");
            person.setBiography(limit(biography, 500));
            person.setVersion((Integer) data.get("version"));
            person.setProfileImageUrl(selectImageUrl((List<Map>) data.get("profile"), "profile", "profile"));
            person.setLastModified(toDate(data, "last_modified_at", "yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new MovieDbException("Failed to map json for person", e);
        }
    }

    private String limit(String text, int limit) {
        if (text == null || text.length() < limit) {
            return text;
        }
        return text.substring(0, limit);
    }


    public Roles mapToRole(String roleString) {
        if (roleString.equals("Actor")) {
            return Roles.ACTS_IN;
        }
        if (roleString.equals("Director")) {
            return Roles.DIRECTED;
        }
        return null;
    }
}
