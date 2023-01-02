/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.neo4j.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.properties.HasModifiers;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.Architectures;
import org.apiguardian.api.API;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

/**
 * Architecture tests replacing the jQAssistant tests.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ArchitectureTest {

	private static final DescribedPredicate<JavaClass> INTERNAL_API_PREDICATE = new DescribedPredicate<>("Is internal API") {
		@Override
		public boolean apply(JavaClass input) {
			API.Status status = input.getAnnotationOfType(API.class).status();
			return "INTERNAL".equals(status.name());
		}
	};

	private JavaClasses sdnClasses;

	@BeforeAll
	void importCorePackage() {
		sdnClasses = new ClassFileImporter()
				.withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
				.importPackages("org.springframework.data.neo4j..");


	}

	@DisplayName("Non abstract, public classes that are only part of internal API must be final")
	@Test
	void finalInternalAPIPublicClasses() {
		ArchRule rule = ArchRuleDefinition.classes().that().areAnnotatedWith(API.class)
				.and().arePublic()
				.and().areTopLevelClasses()
				.and(DescribedPredicate.not(HasModifiers.Predicates.modifier(JavaModifier.ABSTRACT)))
				.and(INTERNAL_API_PREDICATE)
				.should().haveModifier(JavaModifier.FINAL);
		rule.check(sdnClasses);
	}


	@DisplayName("@API Guardian annotations must not be used on fields")
	@Test
	void apiAnnotationsNotOnFields() {

		ArchRule rule = ArchRuleDefinition.fields().should()
				.notBeAnnotatedWith(API.class);

		rule.check(sdnClasses);
	}

	@DisplayName("The mapping package must not depend on any other SDN packages than schema and convert")
	@Test
	void mappingPackageDependencies() {

		Architectures.layeredArchitecture()
				.layer("mapping").definedBy("org.springframework.data.neo4j.core.mapping", "org.springframework.data.neo4j.core.mapping.callback")
				.layer("schema or conversion").definedBy("org.springframework.data.neo4j.core.schema", "org.springframework.data.neo4j.core.convert")
				.layer("everything outside SDN").definedBy(new DescribedPredicate<>("classes outside SDN") {
					@Override
					public boolean apply(JavaClass input) {
						return !input.getPackageName().startsWith("org.springframework.data.neo4j");
					}
				})
				.whereLayer("mapping").mayOnlyAccessLayers("schema or conversion", "everything outside SDN")
				.withOptionalLayers(true)
				.check(sdnClasses);
	}

	@DisplayName("The public support packages must not depend directly on the mapping package")
	@Test
	void publicPackagesMustNotDependOnMappingPackage() {

		ArchRuleDefinition.classes().that().resideInAnyPackage("org.springframework.data.neo4j.core.convert",
						"org.springframework.data.neo4j.core.schema",
						"org.springframework.data.neo4j.core.support",
						"org.springframework.data.neo4j.core.transaction")
				.should()
				.onlyDependOnClassesThat()
				.resideOutsideOfPackages("org.springframework.data.neo4j.core.mapping", "org.springframework.data.neo4j.core.mapping.callback")
				.orShould()
				.dependOnClassesThat().haveFullyQualifiedName("org.springframework.data.neo4j.core.mapping.Neo4jPersistentProperty")
				.check(sdnClasses);

	}
}
