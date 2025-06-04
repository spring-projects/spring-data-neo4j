/*
 * Copyright 2011-2025 the original author or authors.
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
package org.springframework.data.neo4j.core.mapping.callback;

import java.util.Arrays;
import java.util.HashSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.core.Ordered;
import org.springframework.data.auditing.ReactiveIsNewAwareAuditingHandler;
import org.springframework.data.mapping.context.PersistentEntities;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Michael J. Simons
 */
class ReactiveAuditingBeforeBindCallbackTests {

	ReactiveIsNewAwareAuditingHandler spyOnHandler;

	ReactiveAuditingBeforeBindCallback callback;

	@BeforeEach
	void setUp() {

		Neo4jMappingContext mappingContext = new Neo4jMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(Sample.class, ImmutableSample.class)));
		mappingContext.initialize();

		ReactiveIsNewAwareAuditingHandler originalHandler = new ReactiveIsNewAwareAuditingHandler(
				new PersistentEntities(Arrays.asList(mappingContext)));
		this.spyOnHandler = spy(originalHandler);
		this.callback = new ReactiveAuditingBeforeBindCallback(() -> this.spyOnHandler);
	}

	@Test
	void rejectsNullAuditingHandler() {

		assertThatIllegalArgumentException().isThrownBy(() -> new ReactiveAuditingBeforeBindCallback(null));
	}

	@Test
	void triggersCreationMarkForObjectWithEmptyId() {

		Sample sample = new Sample();
		StepVerifier.create(this.callback.onBeforeBind(sample)).expectNextMatches(s -> {
			Sample auditedObject = (Sample) s;
			return auditedObject.created != null && auditedObject.modified != null;
		}).verifyComplete();

		verify(this.spyOnHandler, times(1)).markCreated(sample);
		verify(this.spyOnHandler, times(0)).markModified(any());
	}

	@Test
	void triggersModificationMarkForObjectWithSetId() {

		Sample sample = new Sample();
		sample.id = "id";
		sample.version = 1L;

		StepVerifier.create(this.callback.onBeforeBind(sample)).expectNextMatches(s -> {
			Sample auditedObject = (Sample) s;
			return auditedObject.created == null && auditedObject.modified != null;
		}).verifyComplete();

		verify(this.spyOnHandler, times(0)).markCreated(any());
		verify(this.spyOnHandler, times(1)).markModified(sample);
	}

	@Test
	void hasExplicitOrder() {

		assertThat(this.callback).isInstanceOf(Ordered.class);
		assertThat(this.callback.getOrder()).isEqualTo(100);
	}

	@Test
	void propagatesChangedInstanceToEvent() {

		ImmutableSample sample = new ImmutableSample();

		ImmutableSample newSample = new ImmutableSample();
		ReactiveIsNewAwareAuditingHandler handler = mock(ReactiveIsNewAwareAuditingHandler.class);
		doReturn(Mono.just(newSample)).when(handler).markAudited(eq(sample));

		ReactiveAuditingBeforeBindCallback localCallback = new ReactiveAuditingBeforeBindCallback(() -> handler);
		StepVerifier.create(localCallback.onBeforeBind(sample)).expectNext(newSample);
	}

}
