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
 * By default the OGM will map enum arrays to and from
 * the string arrays with values returned by enum.name()
 *
 * enum.name() is preferred to enum.ordinal() because it
 * is (slightly) safer: a persisted enum have to be renamed
 * to break its database mapping, whereas if its ordinal
 * was persisted instead, the mapping would be broken
 * simply by changing the declaration order in the enum set.
 *
 * @author Luanne Misquitta
 */

public class EnumArrayStringConverter implements AttributeConverter<Enum[],String[]> {

    private final Class<? extends Enum> enumClass;

    public EnumArrayStringConverter(Class<? extends Enum> enumClass) {
        this.enumClass = enumClass;
    }

    @Override
    public String[] toGraphProperty(Enum[] value) {
        if(value==null) {
            return null;
        }
        String[] values = new String[(value.length)];
        int i=0;
        for(Enum e : value) {
            values[i++]=e.name();
        }
        return values;
    }

    @Override
    public Enum[] toEntityAttribute(String[] stringValues) {
        if(stringValues==null) {
            return null;
        }
        Enum[] values = (Enum[])Array.newInstance(enumClass,stringValues.length);
        int i=0;
        for(String value : stringValues) {
            values[i++] = Enum.valueOf(enumClass, value.toString());
        }
        return values;
    }
}
