/*
 * Copyright 2010 the original author or authors.
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

package org.springframework.datastore.graph.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.datastore.graph.api.Direction;
import org.springframework.datastore.graph.api.NodeBacked;
import org.springframework.datastore.graph.api.RelationshipBacked;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GraphInactive {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Entity {
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface Relationship {
			
			String type();
			
			Direction direction() default Direction.OUTGOING;

			Class<? extends NodeBacked> elementClass() default NodeBacked.class;	
		}
		
		@Retention(RetentionPolicy.RUNTIME)
		@Target(ElementType.FIELD)
		public @interface RelationshipEntity {
			String type();
			
			Direction direction() default Direction.OUTGOING;

			Class<? extends RelationshipBacked> elementClass() default RelationshipBacked.class;	
		}
		
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	public @interface Relationship {
		
		String type() default "";
		
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
