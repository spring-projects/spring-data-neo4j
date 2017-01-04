package org.springframework.data.neo4j.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.reflect.FieldUtils;

/**
 * 
 * @author Eric Spiegelberg
 */
public class AnnotationUtils {

	private AnnotationUtils() {
	}
	
	public static List<Field> findFieldsWithAnnotation(Object o, Class<? extends Object> annotationClass) {
		
		List<Field> results = new ArrayList<Field>();
	
		List<Field> fields = FieldUtils.getAllFieldsList(o.getClass());
		
		for (Field field : fields) {
			
			Annotation annotations[] = field.getAnnotations();
			for (Annotation annotation : annotations) {

				if (annotation.annotationType().equals(annotationClass)) {

					results.add(field);
					
				}
				
			}
			
		}

		return results;

	}
	
}