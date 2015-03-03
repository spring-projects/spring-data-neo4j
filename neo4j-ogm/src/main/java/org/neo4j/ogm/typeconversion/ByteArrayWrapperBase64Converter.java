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

package org.neo4j.ogm.typeconversion;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.ArrayUtils;

/**
 * By default the OGM will map Byte[] wrapped byte[] objects to Base64
 * String values when being stored as a node / relationship property
 *
 * The conversion between the primitive byte[] class and its wrapper
 * Byte[] means that this converter is slightly slower than
 * using the ByteArray64Converter, which works with primitive
 * byte arrays directly.
 */
public class ByteArrayWrapperBase64Converter implements AttributeConverter<Byte[], String> {

    @Override
    public String toGraphProperty(Byte[] value) {
        if (value == null) return null;
        return Base64.encodeBase64String(ArrayUtils.toPrimitive(value));
    }

    @Override
    public Byte[] toEntityAttribute(String value) {
        if (value == null) return null;
        byte[] bytes = Base64.decodeBase64(value);
        Byte[] wrapper = new Byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            wrapper[i] = Byte.valueOf(bytes[i]);  // preferable to new Byte(..) hence not using Apache toObject()
        }
        return wrapper;
    }

}
