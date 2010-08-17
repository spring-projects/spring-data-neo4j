package org.springframework.persistence.graph;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface Graph {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Entity {
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface Relationship {	
		}
		
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Relationship {

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface StartNode {
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface EndNode {
		}
		
	}
	
}
