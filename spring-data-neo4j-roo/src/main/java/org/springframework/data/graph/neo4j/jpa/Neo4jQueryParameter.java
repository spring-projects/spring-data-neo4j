/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.jpa;

import javax.persistence.Parameter;
/**
 * @author Michael Hunger
 * @since 11.09.2010
 */
public class Neo4jQueryParameter<T> implements Parameter<T> {
    private final Class<T> type;
    private final String name;
    private final Integer position;

    public Neo4jQueryParameter(Class<T> type, String name, Integer position) {
        this.type = type;
        this.name = name;
        this.position = position;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public Class<T> getParameterType() {
        return type;
    }

    public static Parameter<?> param(String name) { return new Neo4jQueryParameter<Object>(null,name,null);}
    public static Parameter<?> param(Integer position) { return new Neo4jQueryParameter<Object>(null,null,position);}
    public static <T> Parameter<T> param(Class<T> type, String name, Integer position) { return new Neo4jQueryParameter<T>(type,name,position);}
}
