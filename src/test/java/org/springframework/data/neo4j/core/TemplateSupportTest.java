/*
 * Copyright 2011-present the original author or authors.
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
package org.springframework.data.neo4j.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * @author Michael J. Simons
 */
class TemplateSupportTest {

	static class A {
	}

	static class B {
	}

	static class A2 extends A {
	}

	static class A3 extends A {
	}

	static class A4 extends A {
	}

	static class AA2 extends A2 {
	}

	interface IA {
	}

	interface IB {
	}

	static class B1 implements IA {
	}

	static class B2 implements IA {
	}

	static class B3 implements IA, IB {
	}

	static class B4 implements IA, IB {
	}

	@Test
	void shouldFindCommonElementTypeOfHeterousCollection() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new A(), new A(), new A()));
		assertThat(type).isNotNull().isEqualTo(A.class);
	}

	@Test
	void shouldNotFailWithNull() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new A(), null, new A()));
		assertThat(type).isNotNull().isEqualTo(A.class);
	}

	@Test
	void shouldNotFailWithNullInput() {

		Class<?> type = TemplateSupport.findCommonElementType(null);
		assertThat(type).isNull();
	}

	@Test
	void shouldNotFailWithEmptyInput() {

		Class<?> type = TemplateSupport.findCommonElementType(Collections.emptyList());
		assertThat(type).isEqualTo(TemplateSupport.EmptyIterable.class);
	}

	@Test
	void shouldFindCommonElementTypeOfHumongousCollection() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new A2(), new A3(), new A4()));
		assertThat(type).isNotNull().isEqualTo(A.class);
	}

	@Test
	void shouldFindCommonElementTypeOfHumongousDeepCollection() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new A2(), new AA2(), new A3(), new A4()));
		assertThat(type).isNotNull().isEqualTo(A.class);
	}

	@Test
	void shouldFindCommonElementTypeOfHumongousInterfaceCollection() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new B1(), new B2()));
		assertThat(type).isNotNull().isEqualTo(IA.class);

		type = TemplateSupport.findCommonElementType(Arrays.asList(new B1(), new B2(), new B3()));
		assertThat(type).isNotNull().isEqualTo(IA.class);
	}

	@Test
	void shouldNotFindAmbiguousInterface() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new B3(), new B4()));
		assertThat(type).isNull();
	}

	@Test
	void shouldNotFindCommonElementTypeWhenThereIsNone() {

		Class<?> type = TemplateSupport.findCommonElementType(Arrays.asList(new A(), new A(), new B()));
		assertThat(type).isNull();

		type = TemplateSupport.findCommonElementType(Arrays.asList(new A(), new B(), new A()));
		assertThat(type).isNull();

		type = TemplateSupport.findCommonElementType(Arrays.asList(new B(), new A(), new A()));
		assertThat(type).isNull();
	}
}
