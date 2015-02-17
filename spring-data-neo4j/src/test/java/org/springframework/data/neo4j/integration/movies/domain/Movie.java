package org.springframework.data.neo4j.integration.movies.domain;

public class Movie extends AbstractEntity {

    private String title;
    private String[] tags;
    private byte[] image;

    public Movie() {
    }

    public Movie(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public String[] getTags() {
        return tags;
    }

    public void setTags(String[] tags) {
        this.tags = tags;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }
}
