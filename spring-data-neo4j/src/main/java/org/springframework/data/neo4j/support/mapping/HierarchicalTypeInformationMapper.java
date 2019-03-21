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

import org.springframework.data.convert.TypeInformationMapper;
import org.springframework.data.neo4j.mapping.Neo4jPersistentEntity;
import org.springframework.data.util.TypeInformation;

/**
 * @author mh
 * @since 20.02.12
 */
public class HierarchicalTypeInformationMapper implements TypeInformationMapper {
    private final Neo4jMappingContext ctx;

    public HierarchicalTypeInformationMapper(Neo4jMappingContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public TypeInformation<?> resolveTypeFrom(Object alias) {
        final Neo4jPersistentEntity<?> entity = ctx.getPersistentEntity(alias);
        if (entity == null) return null;
        return entity.getTypeInformation();
    }

    @Override
    public Object createAliasFor(TypeInformation<?> type) {
        final Neo4jPersistentEntityImpl<?> entity = ctx.getPersistentEntity(type);
        if (entity != null) return entity.getEntityType();
        return null;
    }
}
