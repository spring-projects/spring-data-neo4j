/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
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

import java.io.Serializable;

import org.neo4j.ogm.metadata.ClassInfo;
import org.neo4j.ogm.metadata.FieldInfo;
import org.neo4j.ogm.metadata.MetaData;
import org.springframework.data.repository.core.support.AbstractEntityInformation;

/**
 * @author Mark Angrish
 * @author Mark Paluch
 */
public class GraphEntityInformation<T, ID extends Serializable> extends AbstractEntityInformation<T, ID> {

	private final MetaData metaData;

	public GraphEntityInformation(MetaData metaData, Class<T> type) {
		super(type);
		this.metaData = metaData;
	}

	@Override
	@SuppressWarnings("unchecked")
	public ID getId(T entity) {
		final ClassInfo classInfo = metaData.classInfo(getJavaType().getName());
		final FieldInfo primaryIndex = classInfo.primaryIndexField();

		return (ID) getId(entity, classInfo, primaryIndex);
	}

	private Object getId(T entity, ClassInfo classInfo, FieldInfo primaryIndex) {
		if (primaryIndex != null) {
			return classInfo.propertyField(primaryIndex.getName()).readProperty(entity);
		} else {
			return classInfo.propertyField(classInfo.identityField().getName()).readProperty(entity);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<ID> getIdType() {
		final FieldInfo primaryIndex = metaData.classInfo(getJavaType().getName()).primaryIndexField();

		if (primaryIndex != null) {
			return (Class<ID>) primaryIndex.convertedType();
		}
		return (Class<ID>) Long.class;
	}

}
