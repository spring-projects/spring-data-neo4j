/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.typeconversion;

import org.apache.commons.codec.binary.Base64;

/**
 * By default the OGM will map byte[] objects to Base64
 * String values when being stored as a node / relationship property
 */
public class ByteArrayBase64Converter implements AttributeConverter<byte[], String> {

    @Override
    public String toGraphProperty(byte[] value) {
        if (value == null) return null;
        return Base64.encodeBase64String(value);
    }

    @Override
    public byte[] toEntityAttribute(String value) {
        if (value == null) return null;
        return Base64.decodeBase64(value);
    }

}
