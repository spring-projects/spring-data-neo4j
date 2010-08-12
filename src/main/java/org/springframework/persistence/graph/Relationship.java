package org.springframework.persistence.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.persistence.test.Person;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Relationship {
	
	String type();
	
	Direction direction();

	Class<?> elementClass() default Void.class;

}
