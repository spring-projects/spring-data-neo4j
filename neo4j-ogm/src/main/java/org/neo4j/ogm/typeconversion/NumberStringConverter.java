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

/**
 * The NumberStringConverter can be used to convert any java object that extends
 * java.lang.Number to and from its String representation.
 *
 * By default, the OGM will automatically convert BigInteger and BigDecimal
 * entity attributes using this converter.
 *
 */
public class NumberStringConverter implements AttributeConverter<Number, String> {

    private final Class<? extends Number> numberClass;

    public NumberStringConverter(Class<? extends Number> numberClass) {
        this.numberClass = numberClass;
    }

    @Override
    public String toGraphProperty(Number value) {
        if (value == null) return null;
        return value.toString();
    }

    @Override
    public Number toEntityAttribute(String value) {
        if (value == null) return null;
        try {
            return numberClass.getDeclaredConstructor(String.class).newInstance(value);
        } catch (Exception e) {
            throw new RuntimeException("Conversion failed!", e);
        }
    }
}
