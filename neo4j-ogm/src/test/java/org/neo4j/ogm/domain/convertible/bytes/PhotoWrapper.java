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

package org.neo4j.ogm.domain.convertible.bytes;

import org.neo4j.ogm.annotation.typeconversion.Convert;
import org.neo4j.ogm.typeconversion.ByteArrayWrapperBase64Converter;

// like a photo but image uses Byte[] instead of byte[]
public class PhotoWrapper {

    Long id;

    // user-defined converter
    @Convert(ByteArrayWrapperBase64Converter.class)
    private Byte[] image;


    // should use default converter
    public Byte[] getImage() {
        return image;
    }

    // should use default converter
    public void setImage(Byte[] image) {
        this.image = image;
    }
}
