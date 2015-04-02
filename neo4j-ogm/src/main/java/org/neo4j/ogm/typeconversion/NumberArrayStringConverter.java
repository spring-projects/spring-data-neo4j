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

import java.lang.reflect.Array;

/**
 * The NumberStringConverter can be used to convert any java object array containing values that extend
 * java.lang.Number to and from its String array representation.
 *
 * By default, the OGM will automatically convert arrays of BigInteger and BigDecimal
 * entity attributes using this converter.
 *
 * @author Luanne Misquitta
 */
public class NumberArrayStringConverter implements AttributeConverter<Number[], String[]> {

    private final Class<? extends Number> numberClass;

    public NumberArrayStringConverter(Class<? extends Number> numberClass) {
        this.numberClass = numberClass;
    }


    @Override
    public String[] toGraphProperty(Number[] value) {
        if (value == null) {
            return null;
        }
        String[] values = new String[(value.length)];

        int i = 0;
        for (Number num : value) {
            values[i++] = num.toString();
        }
        return values;
    }

    @Override
    public Number[] toEntityAttribute(String[] stringValues) {
        if (stringValues == null) {
            return null;
        }
        Number[] values = (Number[])Array.newInstance(numberClass,stringValues.length);

        int i = 0;
        try {
            for (String num : stringValues) {
                values[i++] =numberClass.getDeclaredConstructor(String.class).newInstance(num);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
       return values;
    }
}
