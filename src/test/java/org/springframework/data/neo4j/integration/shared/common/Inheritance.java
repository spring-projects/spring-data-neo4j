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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * @author Gerrit Meier
 * @author Michael J. Simons
 */
public class Inheritance {

	/**
	 * An interface as someone would define in an api package
	 */
	// tag::interface1[]
	public interface SomeInterface { // <.>

		String getName();

		SomeInterface getRelated();
	}

	// end::interface1[]

	/**
	 * Implementation of the above, to be found in a Neo4j or Mongo or whatever module.
	 */
	// tag::interface1[]
	@Node("SomeInterface") // <.>
	public static class SomeInterfaceEntity implements SomeInterface {

		@Id @GeneratedValue private Long id;

		private final String name;

		private SomeInterface related;

		public SomeInterfaceEntity(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public SomeInterface getRelated() {
			return related;
		}
		// end::interface1[]

		public Long getId() {
			return id;
		}

		public void setRelated(SomeInterface related) {
			this.related = related;
		}

		// tag::interface1[]
	}
	// end::interface1[]

	/**
	 * A case where the label was specified on the interface, unsure if this is meaningful
	 */
	// tag::interface2[]
	@Node("PrimaryLabelWN") // <.>
	public interface SomeInterface2 {

		String getName();

		SomeInterface2 getRelated();
	}

	// end::interface2[]

	/**
	 * Implementation of the above
	 */
	// tag::interface2[]
	public static class SomeInterfaceEntity2 implements SomeInterface2 {

		// Overrides omitted for brevity
		// end::interface2[]
		@Id @GeneratedValue private Long id;

		private final String name;

		private SomeInterface2 related;

		public SomeInterfaceEntity2(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public SomeInterface2 getRelated() {
			return related;
		}

		public void setRelated(SomeInterface2 related) {
			this.related = related;
		}
		// tag::interface2[]
	}
	// end::interface2[]

	/**
	 * Concrete interface name here, `@Node` is required, label can be omitted in that case
	 */
	// tag::interface3[]
	@Node("SomeInterface3") // <.>
	public interface SomeInterface3 {

		String getName();

		SomeInterface3 getRelated();
	}

	// end::interface3[]

	/**
	 * One implementation of the above.
	 */
	// tag::interface3[]
	@Node("SomeInterface3a") // <.>
	public static class SomeInterfaceImpl3a implements SomeInterface3 {

		// Overrides omitted for brevity
		// end::interface3[]
		@Id
		@GeneratedValue
		private Long id;

		private SomeInterfaceImpl3b related;

		private final String name;

		public SomeInterfaceImpl3a(String name) {
			this.name = name;
		}

		@Override
		public SomeInterface3 getRelated() {
			return related;
		}

		@Override
		public String getName() {
			return name;
		}

		// tag::interface3[]
	}
	// end::interface3[]

	/**
	 * Another implementation of the above.
	 */
	// tag::interface3[]
	@Node("SomeInterface3b") // <.>
	public static class SomeInterfaceImpl3b implements SomeInterface3 {

		// Overrides omitted for brevity
		// end::interface3[]
		@Id
		@GeneratedValue
		private Long id;

		private SomeInterfaceImpl3a related;

		private final String name;

		public SomeInterfaceImpl3b(String name) {
			this.name = name;
		}

		@Override
		public SomeInterface3 getRelated() {
			return related;
		}

		@Override
		public String getName() {
			return name;
		}

		// tag::interface3[]
	}

	// end::interface3[]


	/**
	 * A thing having different relationships with the same type.
	 */
	// tag::interface3[]
	@Node
	public static class ParentModel { // <.>

		@Id
		@GeneratedValue
		private Long id;

		private SomeInterface3 related1; // <.>

		private SomeInterface3 related2;
		// end::interface3[]

		private final String name;

		public ParentModel(String name) {
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public SomeInterface3 getRelated1() {
			return related1;
		}

		public void setRelated1(SomeInterface3 related1) {
			this.related1 = related1;
		}

		public SomeInterface3 getRelated2() {
			return related2;
		}

		public void setRelated2(SomeInterface3 related2) {
			this.related2 = related2;
		}

		// tag::interface3[]
	}
	// end::interface3[]

	/**
	 * A holder for a list of different interface implementations, see GH-2262.
	 */
	@Node
	public static class ParentModel2 {

		@Id
		@GeneratedValue
		private Long id;

		private List<SomeInterface3> isRelatedTo;

		public Long getId() {
			return id;
		}

		public List<SomeInterface3> getIsRelatedTo() {
			return isRelatedTo;
		}

		public void setIsRelatedTo(List<SomeInterface3> isRelatedTo) {
			this.isRelatedTo = isRelatedTo;
		}
	}

	/**
	 * Interface to get implemented with the one below.
	 */
	@Node("Mix1")
	public interface MixIt1 {
		String getName();
	}

	/**
	 * Interface to get implemented with one above.
	 */
	@Node("Mix2")
	public interface MixIt2 {
		String getValue();
	}

	/**
	 * Implements two interfaces
	 */
	@Node
	public static class Mix1AndMix2 implements MixIt1, MixIt2 {

		@Id
		@GeneratedValue
		private Long id;

		private final String name;
		private final String value;

		public Mix1AndMix2(String name, String value) {
			this.name = name;
			this.value = value;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public String getValue() {
			return value;
		}
	}
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

		@Relationship("CONNECTED")
		public List<BaseClass> others;

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

		@RelationshipId
		private Long id;

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

		@RelationshipId
		private Long id;

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

		@RelationshipId
		private Long id;

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

	/**
	 * Base entity for GH-2138 generic relationships tests
	 */
	@Node("Entity")
	public static abstract class Entity {
		@org.springframework.data.annotation.Id
		@GeneratedValue
		public Long id;
		public String name;

		@Relationship(type = "IS_CHILD")
		public Entity parent;
	}

	/**
	 * company
	 */
	@Node("Company")
	public static class Company extends Entity {}

	/**
	 * site
	 */
	@Node("Site")
	public static class Site extends Entity {}

	/**
	 * building
	 */
	@Node("Building")
	public static class Building extends Entity {}

	/**
	 * Base entity for GH-2138 generic relationship in child class tests
	 */
	@Node
	public static abstract class BaseEntity {
		@Id
		@GeneratedValue
		public Long id;
		public String name;
	}

	/**
	 * BaseTerritory
	 */
	@Node
	public static abstract class BaseTerritory extends BaseEntity {
		public String nameEs;
		public final String nameEn;

		public BaseTerritory(String nameEn) {
			this.nameEn = nameEn;
		}

		public String getNameEn() {
			return nameEn;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			BaseTerritory that = (BaseTerritory) o;
			return nameEn.equals(that.nameEn);
		}

		@Override
		public int hashCode() {
			return Objects.hash(nameEn);
		}
	}

	/**
	 * GenericTerritory
	 */
	@Node
	public static class GenericTerritory extends BaseTerritory {
		public GenericTerritory(String nameEn) {
			super(nameEn);
		}
	}

	/**
	 * Country
	 */
	@Node
	public static class Country extends BaseTerritory {
		public final String countryProperty;

		@Relationship(type = "LINK", direction = Relationship.Direction.OUTGOING)
		public Set<BaseTerritory> relationshipList = new HashSet<>();

		public Country(String nameEn, String countryProperty) {
			super(nameEn);
			this.countryProperty = countryProperty;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}
			Country country = (Country) o;
			return countryProperty.equals(country.countryProperty);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), countryProperty);
		}
	}

	/**
	 * Continent
	 */
	@Node
	public static class Continent extends BaseTerritory {
		public final String continentProperty;

		public Continent(String nameEn, String continentProperty) {
			super(nameEn);
			this.continentProperty = continentProperty;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			if (!super.equals(o)) {
				return false;
			}
			Continent continent = (Continent) o;
			return continentProperty.equals(continent.continentProperty);
		}

		@Override
		public int hashCode() {
			return Objects.hash(super.hashCode(), continentProperty);
		}
	}

	/**
	 * A parent object for some territories, used to test whether those are loaded correct in a polymorphic way.
	 */
	@Node
	public static class Division extends BaseEntity {

		@Relationship
		List<BaseTerritory> isActiveIn;

		public List<BaseTerritory> getIsActiveIn() {
			return isActiveIn;
		}

		public void setIsActiveIn(
				List<BaseTerritory> isActiveIn) {
			this.isActiveIn = isActiveIn;
		}
	}


}
