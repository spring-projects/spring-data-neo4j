package org.springframework.data.neo4j.util;

import java.lang.reflect.Field;
import java.util.List;

import org.neo4j.ogm.annotation.GraphId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.neo4j.annotation.AnnotationUtils;
import org.springframework.util.Assert;

/**
 * 
 * @author Eric Spiegelberg
 */
public class EntityUtils {

	private static final Logger logger = LoggerFactory.getLogger(EntityUtils.class);
	
	public static <T> Boolean isNew(T entity) {
		
		Boolean result = false;
		
		try {
			
			List<Field> idFields = AnnotationUtils.findFieldsWithAnnotation(entity, GraphId.class);
			Assert.isTrue(idFields.size() <= 1, "Entities must have at most a single @GraphId property");
			
			if (idFields.size() > 0) {

				Field idField = idFields.get(0);
				idField.setAccessible(true);
				
				Object value = idField.get(entity);			
				if (value == null) {
					result = true;
				}

			}

		} catch (IllegalArgumentException | IllegalAccessException e) {			
			logger.warn("Unable to determine if entity " + entity + " is new", e);
			result = null;
		}
		
		return result;
		
	}

}