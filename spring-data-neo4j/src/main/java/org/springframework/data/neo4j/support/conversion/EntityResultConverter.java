/**
 * Copyright 2011 the original author or authors.
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

package org.springframework.data.neo4j.support.conversion;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.neo4j.conversion.DefaultConverter;
import org.springframework.data.neo4j.core.EntityPath;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.data.neo4j.support.path.ConvertingEntityPath;

/**
 * @author mh
 * @since 28.06.11
 */
public class EntityResultConverter<T,R> extends DefaultConverter<T,R> {
    private final Neo4jTemplate ctx;
    private final ConversionService conversionService;

    public EntityResultConverter(Neo4jTemplate ctx) {
        this.ctx = ctx;
        conversionService = this.ctx.getConversionService();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Object doConvert(Object value, Class<?> sourceType, Class targetType) {
        if (ctx.isNodeEntity(targetType)) {
            return ctx.projectTo(toNode(value, sourceType), targetType);
        }
        if (ctx.isRelationshipEntity(targetType)) {
            return ctx.projectTo(toRelationship(value, sourceType), targetType);
        }
        if (EntityPath.class.isAssignableFrom(targetType)) {
            return new ConvertingEntityPath(ctx,toPath(value,sourceType));
        }
        final Object result = super.doConvert(value, sourceType, targetType);

        if (result!=null) return result;

        if (conversionService.canConvert(sourceType, targetType)) {
            return conversionService.convert(value,targetType);
        }
        return result;
    }
}
