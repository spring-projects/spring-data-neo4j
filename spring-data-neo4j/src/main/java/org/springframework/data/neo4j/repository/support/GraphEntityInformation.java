/*
 * Copyright 2011-2021 the original author or authors.
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
