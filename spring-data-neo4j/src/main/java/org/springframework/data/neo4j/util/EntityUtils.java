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
package org.springframework.data.neo4j.util;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.neo4j.ogm.annotation.GraphId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * 
 * @author Eric Spiegelberg
 */
public class EntityUtils {

	private static final Logger logger = LoggerFactory.getLogger(EntityUtils.class);
	
	/**
	 * Determine if the domain entity is new, with "new" being defined as if the entity's GraphId property 
	 * having a non-null value or not.
	 * 
	 * @param entity The domain entity.
	 * @return Returns boolean true is the entity's GraphId property has a null value, boolean false if 
	 * the GraphId property has a non-null value, and Boolean null if an access/argument exception 
	 * occurred and/or if a qualifying GraphId property could not be found.
	 */
	public static <T> Boolean isNew(T entity) {
		
		Boolean result = null;

		List<Field> idFields = FieldUtils.getFieldsListWithAnnotation(entity.getClass(), GraphId.class);			
		Assert.isTrue(idFields.size() == 0 || idFields.size() == 1, "Entities must have at most a single @GraphId property");
		
		if (idFields.size() == 0) {
			
			/* 
			 * No @GraphId was present on the entity, attempt to find the id property by name, as per 
			 * http://docs.spring.io/spring-data/neo4j/docs/current/reference/html/#__graphid_neo4j_id_field:
			 * 
			 * "If the field is simply named 'id' then it is not necessary to annotate it with @GraphId 
			 * as the OGM will use it automatically."
			 */
			Field idField = FieldUtils.getField(entity.getClass(), "id", true);
			if (Long.class == idField.getType()) {
				
				result = getIsNew(entity, idField);

			}
			
		} else if (idFields.size() > 0) {

			Field idField = idFields.get(0);
			
			result = getIsNew(entity, idField);

		}
		
		return result;
		
	}

	protected static Boolean getIsNew(Object entity, Field idField) {
		
		Boolean result = null;
		
		try {
			
			idField.setAccessible(true);				
			Object value = idField.get(entity);
			result = (value == null) ? true : false;
		
		} catch (IllegalArgumentException | IllegalAccessException e) {			
			logger.warn("Unable to determine if entity " + entity + " is new", e);
			result = null;
		}
		
		return result;
		
	}
}