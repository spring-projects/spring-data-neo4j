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

    public void mapToMovie(Map data, Movie movie, String posterFormat) {
        try {
            movie.setTitle((String) data.get("title"));
            movie.setLanguage((String) data.get("original_language"));
            movie.setImdbId((String) data.get("imdb_id"));
            movie.setTagline((String) data.get("tagline"));
            movie.setDescription(limit((String) data.get("overview"), 500));
            movie.setReleaseDate(toDate(data, "release_date", "yyyy-MM-dd"));
            movie.setRuntime((Integer) data.get("runtime"));
            movie.setHomepage((String) data.get("homepage"));
            Map trailer = extractFirstMap(data, "trailers", "youtube");
            if (trailer!=null) movie.setTrailer("http:/youtu.be/" + trailer.get("source"));
            movie.setGenre(extractFirst(data, "genres", "name"));
            movie.setStudio(extractFirst(data,"production_companies", "name"));
            movie.setImageUrl(String.format(posterFormat, data.get("poster_path")));
//            movie.setVersion((Integer)data.get("version"));
//            movie.setLastModified(toDate(data,"last_modified_at","yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new MovieDbException("Failed to map json for movie", e);
        }
    }

    private String selectImageUrl(List<Map> data, final String type, final String size) {
        if (data==null) return null;
        for (Map entry : data) {
            Map image = (Map) entry.get("image");
            if (image.get("type").equals(type) && image.get("size").equals(size)) return (String) image.get("url");
        }
        return null;
    }


    private String extractFirst(Map data, String field, String property) {
        List<Map> inner = (List<Map>) data.get(field);
        if (inner == null || inner.isEmpty()) return null;
        return (String) inner.get(0).get(property);
    }
    private Map extractFirstMap(Map data, String field, String property) {
        Map inner = (Map) data.get(field);
        if (inner == null || inner.isEmpty()) return null;
        List list = (List) inner.get(property);
        if (list == null || list.isEmpty()) return null;
        return (Map) list.get(0);
    }

    private Date toDate(Map data, String field, final String pattern) throws ParseException {
        try {
            String dateString = (String) data.get(field);
            if (dateString == null || dateString.isEmpty()) return null;
            return new SimpleDateFormat(pattern).parse(dateString);
        } catch (Exception e) {
            return null;
        }
    }

/*
    {
        adult: false,
                also_known_as: [ ],
        biography: "From Wikipedia, the free encyclopedia. Robert "Bob" Peterson (born January 1961) is an American animator, screenwriter, director and voice actor, who has worked for Pixar since 1994. His first job was working as a layout artist and animator on Toy Story. He was nominated for an Oscar for his screenplay for Finding Nemo. Peterson is a co-director and the writer of Up, which gained him his second Oscar nomination. He also performed the voices of Roz in Monsters, Inc., Mr. Ray in Finding Nemo, Dug the dog and Alpha the dog in Up[1] and in Dug's Special Mission, and the Janitor in Toy Story 3. Peterson was born in Wooster, Ohio, his family moved to Dover, Ohio, where he graduated from Dover High School. He received his undergraduate degree from Ohio Northern University, and a Master's degree in mechanical engineering from Purdue University in 1986. While attending Purdue, he wrote and illustrated the comic strip Loco Motives for the Purdue Exponent. Prior to coming to Pixar, Peterson worked at Wavefront Technologies and Rezn8 Productions. In 2008, Peterson played the part of Terry Cane, a puppeteer in Dan Scanlon's first feature film Tracy. He also played additional voices in Tokyo Mater in 2008, and the voice of Mr. Ray for the Finding Nemo Submarine Voyage ride at Disneyland Park in 2007. His most recent job at Pixar was voicing the Janitor at Sunnyside Daycare Center in Pixar's 11th film, Toy Story 3, which was released on June 18, 2010. Description above from the Wikipedia article Bob Peterson (animator), licensed under CC-BY-SA, full list of contributors on Wikipedia.",
            birthday: "1961-01-18",
            deathday: "",
            homepage: "",
            id: 10,
            imdb_id: "nm0677037",
            name: "Bob Peterson",
            place_of_birth: "Wooster, Ohio, USA",
            popularity: "0.00222079131937198",
            profile_path: "/13YNM8lBKnK26MYd2Lp3OpU6JdI.jpg",
            images: {
        profiles: [
        {
            aspect_ratio: "0.666666666666667",
                    file_path: "/13YNM8lBKnK26MYd2Lp3OpU6JdI.jpg",
                height: 720,
                id: "5311fd64c3a3682a220029f9",
                iso_639_1: null,
                vote_average: 0,
                vote_count: 0,
                width: 480
        }
        ]
    }
    }
    */

    public void mapToPerson(Map data, Person person, String profileFormat) {
        try {
            person.setName((String) data.get("name"));
            person.setBirthday(toDate(data, "birthday", "yyyy-MM-dd"));
            person.setBirthplace((String) data.get("place_of_birth"));
            String biography = (String) data.get("biography");
            person.setBiography(limit(biography,500));
            person.setProfileImageUrl(String.format(profileFormat,(data.get("profile_path"))));
//            person.setVersion((Integer) data.get("version"));
//            person.setLastModified(toDate(data,"last_modified_at","yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            throw new MovieDbException("Failed to map json for person", e);
        }
    }

    private String limit(String text, int limit) {
        if (text==null || text.length() < limit) return text;
        return text.substring(0,limit);
    }

    public Roles mapToRole(String roleString) {
        if (roleString==null || roleString.equals("Actor")) {
            return Roles.ACTS_IN;
        }
        if (roleString.equals("Director")) {
            return Roles.DIRECTED;
        }
        return null;
    }

/*
    {
adult: false,
backdrop_path: "/8qk8f84gfkfoD3tfBVdG8b4ZOvU.jpg",
belongs_to_collection: {
id: 8384,
name: "Proletariat Collection",
poster_path: null,
backdrop_path: "/ibLeWo3X3dVo4rxvn4m3y90ms1I.jpg"
},
budget: 0,
genres: [
{
id: 18,
name: "Drama"
},
{
id: 10769,
name: "Foreign"
}
],
homepage: "",
id: 2,
imdb_id: "tt0094675",
original_language: "en",
original_title: "Ariel",
overview: "Taisto Kasurinen is a Finnish coal miner whose father has just committed suicide and who is framed for a crime he did not commit. In jail, he starts to dream about leaving the country and starting a new life. He escapes from prison but things don't go as planned...",
popularity: "0.113674818134343",
poster_path: "/w0NzAc4Lv6euPtPAmsdEf0ZCF8C.jpg",
production_companies: [
{
name: "Villealfa Filmproduction Oy",
id: 2303
},
{
name: "Finnish Film Foundation",
id: 2396
}
],
production_countries: [
{
iso_3166_1: "FI",
name: "Finland"
}
],
release_date: "1988-10-21",
revenue: 0,
runtime: 69,
spoken_languages: [
{
iso_639_1: "de",
name: "Deutsch"
},
{
iso_639_1: "fi",
name: "suomi"
}
],
status: "Released",
tagline: "",
title: "Ariel",
video: false,
vote_average: 6.5,
vote_count: 5
}
     */
}
