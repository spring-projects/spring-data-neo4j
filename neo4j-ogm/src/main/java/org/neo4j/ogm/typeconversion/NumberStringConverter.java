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

/**
 * The NumberStringConverter can be used to convert any java object that extends
 * java.lang.Number to and from its String representation.
 *
 * By default, the OGM will automatically convert BigInteger and BigDecimal
 * entity attributes using this converter.
 *
 * @author Vince Bickers
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
