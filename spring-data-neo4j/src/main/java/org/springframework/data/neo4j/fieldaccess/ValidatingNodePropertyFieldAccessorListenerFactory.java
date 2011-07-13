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

package org.springframework.data.neo4j.fieldaccess;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.PropertyContainer;
import org.springframework.data.neo4j.core.GraphBacked;
import org.springframework.data.neo4j.support.GraphDatabaseContext;

import javax.validation.Constraint;
import javax.validation.ConstraintViolation;
import javax.validation.ValidationException;
import javax.validation.Validator;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;


class ValidatingNodePropertyFieldAccessorListenerFactory<T extends GraphBacked<?>> implements FieldAccessorListenerFactory<T> {

    private final GraphDatabaseContext graphDatabaseContext;

    ValidatingNodePropertyFieldAccessorListenerFactory(final GraphDatabaseContext graphDatabaseContext) {
    	this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    public boolean accept(final Field f) {
        return hasValidationAnnotation(f);
    }

    private boolean hasValidationAnnotation(final Field f) {
        for (Annotation annotation : f.getAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Constraint.class)) return true;
        }
        return false;
    }

    @Override
    public FieldAccessListener<T, ?> forField(Field field) {
        return new ValidatingNodePropertyFieldAccessorListener(field,graphDatabaseContext.getValidator());
    }


    /**
	 * @author Michael Hunger
	 * @since 12.09.2010
	 */
	public static class ValidatingNodePropertyFieldAccessorListener<T extends PropertyContainer> implements FieldAccessListener<GraphBacked<T>, Object> {

	    private final static Log log = LogFactory.getLog( ValidatingNodePropertyFieldAccessorListener.class );
        private String propertyName;
        private Validator validator;
        private Class<?> entityType;

        public ValidatingNodePropertyFieldAccessorListener(final Field field, Validator validator) {
            this.propertyName = field.getName();
            this.entityType = field.getDeclaringClass();
            this.validator = validator;
        }

	    @Override
        public void valueChanged(GraphBacked<T> graphBacked, Object oldVal, Object newVal) {
            if (validator==null) return;
            Set<ConstraintViolation<T>> constraintViolations = validator.validateValue((Class<T>)entityType, propertyName, newVal);
            if (!constraintViolations.isEmpty()) throw new ValidationException("Error validating field "+propertyName+ " of "+entityType+": "+constraintViolations);
        }
    }
}
