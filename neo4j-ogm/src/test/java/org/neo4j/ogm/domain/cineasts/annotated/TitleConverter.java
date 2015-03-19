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

package org.neo4j.ogm.domain.cineasts.annotated;

import org.neo4j.ogm.typeconversion.AttributeConverter;

import java.util.ArrayList;
import java.util.List;

public class TitleConverter implements AttributeConverter<List<Title>,String[]> {

    @Override
    public String[] toGraphProperty(List<Title> value) {
        if(value==null) {
            return null;
        }
        String[] values = new String[(value.size())];
        int i=0;
        for(Title title : value) {
            values[i++]=title.name();
        }
        return values;
    }

    @Override
    public List<Title> toEntityAttribute(String[] value) {
        List<Title> titles = new ArrayList<>(value.length);
        for(String role : value) {
           titles.add(Title.valueOf(role));
        }
        return titles;
    }
}