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
package org.springframework.data.neo4j.integration.shared.common;

import java.util.List;
import java.util.Objects;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Gerrit Meier
 */
public class Inheritance {

	/**
	 * super base class
	 */
	@Node
	public static abstract class SuperBaseClass {
		@Id @GeneratedValue private Long id;

		public Long getId() {
			return id;
		}
	}

	/**
	 * base class
	 */
	@Node
	public static abstract class BaseClass extends SuperBaseClass {

		private final String name;

		protected BaseClass(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}

	/**
	 * first concrete implementation
	 */
	@Node
	public static class ConcreteClassA extends BaseClass {

		private final String concreteSomething;

		public ConcreteClassA(String name, String concreteSomething) {
			super(name);
			this.concreteSomething = concreteSomething;
		}

		public String getConcreteSomething() {
			return concreteSomething;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConcreteClassA that = (ConcreteClassA) o;
			return concreteSomething.equals(that.concreteSomething);
		}

		@Override
		public int hashCode() {
			return Objects.hash(concreteSomething);
		}
	}

	/**
	 * second concrete implementation
	 */
	@Node
	public static class ConcreteClassB extends BaseClass {

		private final Integer age;

		public ConcreteClassB(String name, Integer age) {
			super(name);
			this.age = age;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConcreteClassB that = (ConcreteClassB) o;
			return age.equals(that.age);
		}

		@Override
		public int hashCode() {
			return Objects.hash(age);
		}
	}

	/**
	 * Base class with explicit primary and additional labels.
	 */
	@Node({ "LabeledBaseClass", "And_another_one" })
	public static abstract class BaseClassWithLabels {

		@Id @GeneratedValue private Long id;
	}

	/**
	 * Class that also has explicit labels
	 */
	@Node({ "ExtendingClassA", "And_yet_more_labels" })
	public static class ExtendingClassWithLabelsA extends BaseClassWithLabels {

		private final String name;

		public ExtendingClassWithLabelsA(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ExtendingClassWithLabelsA that = (ExtendingClassWithLabelsA) o;
			return name.equals(that.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name);
		}
	}

	/**
	 * Another class that also has explicit labels
	 */
	@Node({ "ExtendingClassB", "And_other_labels" })
	public static class ExtendingClassWithLabelsB extends BaseClassWithLabels {

		private final String somethingElse;

		public ExtendingClassWithLabelsB(String somethingElse) {
			this.somethingElse = somethingElse;
		}

		public String getSomethingElse() {
			return somethingElse;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ExtendingClassWithLabelsB that = (ExtendingClassWithLabelsB) o;
			return somethingElse.equals(that.somethingElse);
		}

		@Override
		public int hashCode() {
			return Objects.hash(somethingElse);
		}
	}

	/**
	 * Class that has generic relationships
	 */
	@Node
	public static class RelationshipToAbstractClass {

		@Id @GeneratedValue private Long id;

		@Relationship("HAS") private List<SuperBaseClass> things;

		public void setThings(List<SuperBaseClass> things) {
			this.things = things;
		}

		public List<SuperBaseClass> getThings() {
			return things;
		}
	}

	/**
	 *  Abstract super base class with relationships
	 */
	@Node("SuperBaseClassWithRelationship")
	public static abstract class SuperBaseClassWithRelationship {
		@Id @GeneratedValue private Long id;

		@Relationship("RELATED_TO") private List<ConcreteClassB> boing;

		public void setBoing(List<ConcreteClassB> boing) {
			this.boing = boing;
		}

		public List<ConcreteClassB> getBoing() {
			return boing;
		}
	}

	/**
	 *  Abstract base class with relationships
	 */
	@Node("BaseClassWithRelationship")
	public static abstract class BaseClassWithRelationship extends SuperBaseClassWithRelationship {

		@Relationship("HAS") private List<SuperBaseClass> things;

		public void setThings(List<SuperBaseClass> things) {
			this.things = things;
		}

		public List<SuperBaseClass> getThings() {
			return things;
		}
	}

	/**
	 *  Concrete implementation
	 */
	@Node
	public static class ExtendingBaseClassWithRelationship extends BaseClassWithRelationship {

		@Relationship("SOMETHING_ELSE") private List<ConcreteClassA> somethingConcrete;

		public void setSomethingConcrete(List<ConcreteClassA> somethingConcrete) {
			this.somethingConcrete = somethingConcrete;
		}

		public List<ConcreteClassA> getSomethingConcrete() {
			return somethingConcrete;
		}
	}

	// Same as above but with relationship properties instead of direct relationship links.
	/**
	 *  Abstract super base class with relationship properties
	 */
	@Node("SuperBaseClassWithRelationshipProperties")
	public static abstract class SuperBaseClassWithRelationshipProperties {
		@Id @GeneratedValue private Long id;

		@Relationship("RELATED_TO") private List<ConcreteBRelationshipProperties> boing;

		public void setBoing(List<ConcreteBRelationshipProperties> boing) {
			this.boing = boing;
		}

		public List<ConcreteBRelationshipProperties> getBoing() {
			return boing;
		}
	}

	/**
	 *  Abstract base class with relationship properties
	 */
	@Node("BaseClassWithRelationshipProperties")
	public static abstract class BaseClassWithRelationshipProperties extends SuperBaseClassWithRelationshipProperties {

		@Relationship("HAS") private List<SuperBaseClassRelationshipProperties> things;

		public void setThings(List<SuperBaseClassRelationshipProperties> things) {
			this.things = things;
		}

		public List<SuperBaseClassRelationshipProperties> getThings() {
			return things;
		}
	}

	/**
	 *  Concrete implementation
	 */
	@Node
	public static class ExtendingBaseClassWithRelationshipProperties extends BaseClassWithRelationshipProperties {

		@Relationship("SOMETHING_ELSE") private List<ConcreteARelationshipProperties> somethingConcrete;

		public void setSomethingConcrete(List<ConcreteARelationshipProperties> somethingConcrete) {
			this.somethingConcrete = somethingConcrete;
		}

		public List<ConcreteARelationshipProperties> getSomethingConcrete() {
			return somethingConcrete;
		}
	}

	/**
	 * Relationship properties with target ConcreteClassA.
	 */
	@RelationshipProperties
	public static class ConcreteARelationshipProperties {
		@TargetNode
		private ConcreteClassA target;

		public ConcreteARelationshipProperties(ConcreteClassA target) {
			this.target = target;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConcreteARelationshipProperties that = (ConcreteARelationshipProperties) o;
			return target.equals(that.target);
		}

		@Override
		public int hashCode() {
			return Objects.hash(target);
		}
	}

	/**
	 * Relationship properties with target ConcreteClassB.
	 */
	@RelationshipProperties
	public static class ConcreteBRelationshipProperties {
		@TargetNode
		private ConcreteClassB target;

		public ConcreteBRelationshipProperties(ConcreteClassB target) {
			this.target = target;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ConcreteBRelationshipProperties that = (ConcreteBRelationshipProperties) o;
			return target.equals(that.target);
		}

		@Override
		public int hashCode() {
			return Objects.hash(target);
		}
	}

	/**
	 * Relationship properties with target SuperBaseClass.
	 */
	@RelationshipProperties
	public static class SuperBaseClassRelationshipProperties {
		@TargetNode
		private SuperBaseClass target;

		public SuperBaseClassRelationshipProperties(SuperBaseClass target) {
			this.target = target;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			SuperBaseClassRelationshipProperties that = (SuperBaseClassRelationshipProperties) o;
			return target.equals(that.target);
		}

		@Override
		public int hashCode() {
			return Objects.hash(target);
		}
	}


}
