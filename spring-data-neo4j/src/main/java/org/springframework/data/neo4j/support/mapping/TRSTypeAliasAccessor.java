/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.neo4j.support.mapping;

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.convert.TypeAliasAccessor;
import org.springframework.data.neo4j.core.TypeRepresentationStrategy;

/**
 * @author mh
 * @since 08.10.11
 */
public class TRSTypeAliasAccessor<S extends PropertyContainer> implements TypeAliasAccessor<S> {
    private final TypeRepresentationStrategy<S> typeRepresentationStrategy;

    public TRSTypeAliasAccessor(TypeRepresentationStrategy<S> typeRepresentationStrategy) {
        this.typeRepresentationStrategy = typeRepresentationStrategy;
    }

    @Override
    public Object readAliasFrom(S source) {
        try {
            return typeRepresentationStrategy.readAliasFrom(source);
        } catch (UnsupportedOperationException uoe) {
            return null;
        }
    }

    @Override
    public void writeTypeTo(S sink, Object type) {
        if (type==null) return;
        if (type instanceof StoredEntityType) {
            typeRepresentationStrategy.writeTypeTo(sink, (StoredEntityType) type);
        } else {
            throw new IllegalArgumentException("TypeRepresentationStrategies can only store StoredEntityType instances, not "+type.getClass());
        }
    }

}
