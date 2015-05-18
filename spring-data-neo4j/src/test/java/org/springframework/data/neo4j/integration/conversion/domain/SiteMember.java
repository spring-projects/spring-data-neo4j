/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */
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
