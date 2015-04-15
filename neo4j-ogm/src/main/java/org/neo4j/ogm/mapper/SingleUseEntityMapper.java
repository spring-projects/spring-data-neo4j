/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and licence terms.  Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's licence, as noted in the LICENSE file.
 */

package org.neo4j.ogm.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.neo4j.ogm.entityaccess.DefaultEntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityAccess;
import org.neo4j.ogm.entityaccess.EntityAccessStrategy;
import org.neo4j.ogm.entityaccess.EntityFactory;
import org.neo4j.ogm.entityaccess.PropertyReader;
import org.neo4j.ogm.entityaccess.PropertyWriter;
import org.neo4j.ogm.metadata.MappingException;
import org.neo4j.ogm.metadata.MetaData;
import org.neo4j.ogm.metadata.info.ClassInfo;
import org.neo4j.ogm.session.result.RowModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple graph-to-entity mapper suitable for ad-hoc, one-off mappings.  This doesn't interact with a
 * mapping context or mandate graph IDs on the target types and is not designed for use in the OGM session.
 *
 * @author Adam George
 */
public class SingleUseEntityMapper {

    private static final Logger logger = LoggerFactory.getLogger(SingleUseEntityMapper.class);

    private final EntityAccessStrategy entityAccessStrategy;
    private final EntityFactory entityFactory;
    private final MetaData metadata;

    /**
     * Constructs a new {@link SingleUseEntityMapper} based on the given mapping {@link MetaData}.
     *
     * @param mappingMetaData The {@link MetaData} to use for performing mappings
     */
    public SingleUseEntityMapper(MetaData mappingMetaData, EntityFactory entityFactory) {
        this.metadata = mappingMetaData;
        this.entityFactory = new EntityFactory(mappingMetaData);
        this.entityAccessStrategy = new DefaultEntityAccessStrategy();
    }

    /**
     * Maps a row-based result onto a new instance of the specified type.
     *
     * @param type The {@link Class} denoting the type of object to create
     * @param columnNames The names of the columns in each row of the result
     * @param rowModel The {@link RowModel} containing the data to map
     * @return A new instance of <tt>T</tt> populated with the data in the specified row model
     */
    public <T> T map(Class<T> type, String[] columnNames, RowModel rowModel) {
        Map<String, Object> properties = new HashMap<>();
        for (int i = 0; i < rowModel.getValues().length; i++) {
            properties.put(columnNames[i], rowModel.getValues()[i]);
        }

        T entity = this.entityFactory.newObject(type);
        setPropertiesOnEntity(entity, properties);
        return entity;
    }

    private void setPropertiesOnEntity(Object entity, Map<String, Object> propertyMap) {
        ClassInfo classInfo = resolveClassInfoFor(entity.getClass());
        for (Entry<String, Object> propertyMapEntry : propertyMap.entrySet()) {
            writeProperty(classInfo, entity, propertyMapEntry);
        }
    }

    private ClassInfo resolveClassInfoFor(Class<?> type) {
        ClassInfo classInfo = this.metadata.classInfo(type.getSimpleName());
        if (classInfo != null) {
            return classInfo;
        }
        throw new MappingException("Error mapping to ad-hoc " + type +
                ".  At present, only @QueryResult types that are discovered by the domain entity package scanning can be mapped.");
    }

    // TODO: the following is all pretty much identical to GraphEntityMapper so should probably be refactored
    private void writeProperty(ClassInfo classInfo, Object instance, Map.Entry<String, Object> property) {
        PropertyWriter writer = this.entityAccessStrategy.getPropertyWriter(classInfo, property.getKey());

        if (writer == null) {
            logger.warn("Unable to find property: {} on class: {} for writing", property.getKey(), classInfo.name());
        } else {
            Object value = property.getValue();
            // merge iterable / arrays and co-erce to the correct attribute type
            if (writer.type().isArray() || Iterable.class.isAssignableFrom(writer.type())) {
                PropertyReader reader = this.entityAccessStrategy.getPropertyReader(classInfo, property.getKey().toString());
                if (reader != null) {
                    Object currentValue = reader.read(instance);
                    Class<?> paramType = writer.type();
                    value = paramType.isArray()
                            ? EntityAccess.merge(paramType, (Iterable<?>) value, (Object[]) currentValue)
                            : EntityAccess.merge(paramType, (Iterable<?>) value, (Iterable<?>) currentValue);
                }
            }
            writer.write(instance, value);
        }
    }

}
