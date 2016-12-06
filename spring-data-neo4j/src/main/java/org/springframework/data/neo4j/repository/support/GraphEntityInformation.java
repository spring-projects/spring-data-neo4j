/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.repository.support;

import org.neo4j.ogm.MetaData;
import org.neo4j.ogm.entity.io.EntityAccessManager;
import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

import java.io.Serializable;

/**
 * @author Mark Angrish
 */
public class GraphEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

    private final MetaData metaData;

    public GraphEntityInformation(MetaData metaData, Class<T> type) {
        super(type);
        this.metaData = metaData;
    }

    @Override
    public ID getId(T entity) {
        final ClassInfo classInfo = metaData.classInfo(getJavaType().getName());
        final FieldInfo primaryIndex = classInfo.primaryIndexField();

        if (primaryIndex != null) {
            return (ID) EntityAccessManager.getPropertyReader(classInfo, primaryIndex.getName()).readProperty(entity);
        }
        else {
            return (ID) EntityAccessManager.getPropertyReader(classInfo, classInfo.identityField().getName()).readProperty(entity);
        }
    }

    @Override
    public Class<ID> getIdType() {
        final FieldInfo primaryIndex = metaData.classInfo(getJavaType().getName()).primaryIndexField();

        if (primaryIndex != null) {
            return (Class<ID>) primaryIndex.convertedType();
        }
        return (Class<ID>) Long.class;
    }

}
