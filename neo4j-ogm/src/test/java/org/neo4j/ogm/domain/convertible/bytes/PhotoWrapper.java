/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
