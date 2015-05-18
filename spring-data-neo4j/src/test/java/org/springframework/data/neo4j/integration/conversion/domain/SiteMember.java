package org.springframework.data.neo4j.integration.conversion.domain;

import org.neo4j.ogm.annotation.GraphId;
import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.ByteArrayBase64Converter;

/**
 * @author Adam George
 */
public class SiteMember {

    @GraphId
    private Long id;

    @Convert(ByteArrayBase64Converter.class)
    private byte[] profilePictureData;

    public byte[] getProfilePictureData() {
        return this.profilePictureData;
    }

}
