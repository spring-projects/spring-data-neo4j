package org.springframework.data.neo4j.extensions.domain;

/**
 * @author: Vince Bickers
 */
public class User {

    private Long id;

    //@Convert(ByteArrayBase64Converter.class)
    private byte[] profilePictureData;

    public byte[] getProfilePictureData() {
        return this.profilePictureData;
    }

}
