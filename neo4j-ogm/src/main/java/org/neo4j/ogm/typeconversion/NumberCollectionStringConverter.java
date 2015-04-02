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

import java.util.*;

/**
 * The NumberStringConverter can be used to convert any java object collection containing values that extend
 * java.lang.Number to and from its String array representation.
 *
 * By default, the OGM will automatically convert Collections of BigInteger and BigDecimal
 * entity attributes using this converter.
 *
 * @author Luanne Misquitta
 */
public class NumberCollectionStringConverter implements AttributeConverter<Collection<Number>, String[]>  {

    private final Class<? extends Number> numberClass;
    private final Class<? extends Collection> collectionClass;


    public NumberCollectionStringConverter(Class<? extends Number> numberClass,Class<? extends Collection> collectionClass) {
        this.numberClass = numberClass;
        this.collectionClass = collectionClass;
    }


    @Override
    public String[] toGraphProperty(Collection<Number> value) {
        if (value == null) {
            return null;
        }
        String[] values = new String[(value.size())];

        int i = 0;
        for (Number num : value) {
            values[i++] = num.toString();
        }
        return values;
    }

    @Override
    public Collection<Number> toEntityAttribute(String[] stringValues) {
        if (stringValues == null) {
            return null;
        }
        Collection<Number> values = null;

        if (List.class.isAssignableFrom(collectionClass)) {
            values = new ArrayList<>(stringValues.length);
        } else if (Vector.class.isAssignableFrom(collectionClass)) {
            values = new Vector<>(stringValues.length);
        } else if (Set.class.isAssignableFrom(collectionClass)) {
            values = new HashSet<>(stringValues.length);
        } else {
            return null;
        }
        try {
            for (String value : stringValues) {
                values.add(numberClass.getDeclaredConstructor(String.class).newInstance(value));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return values;
    }
}
