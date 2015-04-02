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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * By default the OGM will map enum collections to and from
 * the string collections containing values returned by enum.name()
 *
 * enum.name() is preferred to enum.ordinal() because it
 * is (slightly) safer: a persisted enum have to be renamed
 * to break its database mapping, whereas if its ordinal
 * was persisted instead, the mapping would be broken
 * simply by changing the declaration order in the enum set.
 *
 * @author Luanne Misquitta
 */

public class EnumCollectionStringConverter implements AttributeConverter<Collection<Enum>, String[]> {

    private final Class<? extends Enum> enumClass;
    private final Class<? extends Collection> collectionClass;

    public EnumCollectionStringConverter(Class<? extends Enum> enumClass, Class<? extends Collection> collectionClass) {
        this.enumClass = enumClass;
        this.collectionClass = collectionClass;
    }

    @Override
    public String[] toGraphProperty(Collection<Enum> value) {
        if (value == null) {
            return null;
        }
        String[] values = new String[(value.size())];
        int i = 0;
        for (Enum e : value) {
            values[i++] = e.name();
        }
        return values;
    }

    @Override
    public Collection<Enum> toEntityAttribute(String[] stringValues) {
        if (stringValues == null) {
            return null;
        }
        Collection<Enum> values;
        if (List.class.isAssignableFrom(collectionClass)) {
            values = new ArrayList<>(stringValues.length);
        } else if (Vector.class.isAssignableFrom(collectionClass)) {
            values = new Vector<>(stringValues.length);
        } else if (Set.class.isAssignableFrom(collectionClass)) {
            values = new HashSet<>(stringValues.length);
        } else {
            return null;
        }
        for (String value : stringValues) {
            values.add(Enum.valueOf(enumClass, value.toString()));
        }
        return values;
    }
}
