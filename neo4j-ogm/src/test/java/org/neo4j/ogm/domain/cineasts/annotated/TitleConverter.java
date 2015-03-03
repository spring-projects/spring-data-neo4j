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