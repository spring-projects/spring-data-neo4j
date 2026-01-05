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
package org.springframework.data.neo4j.aot;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.data.neo4j.core.mapping.callback.AfterConvertCallback;
import org.springframework.data.neo4j.core.mapping.callback.BeforeBindCallback;
import org.springframework.data.neo4j.core.mapping.callback.ReactiveBeforeBindCallback;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.support.UUIDStringGenerator;
import org.springframework.data.neo4j.repository.query.CypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.QuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.ReactiveCypherdslConditionExecutorImpl;
import org.springframework.data.neo4j.repository.query.ReactiveQuerydslNeo4jPredicateExecutor;
import org.springframework.data.neo4j.repository.query.SimpleQueryByExampleExecutor;
import org.springframework.data.neo4j.repository.query.SimpleReactiveQueryByExampleExecutor;
import org.springframework.data.neo4j.repository.support.SimpleNeo4jRepository;
import org.springframework.data.neo4j.repository.support.SimpleReactiveNeo4jRepository;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.data.util.ReactiveWrappers;
import org.springframework.lang.Nullable;

import java.util.Arrays;

/**
 * @author Gerrit Meier
 * @since 7.0.0
 */
public class Neo4jRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {

		hints.reflection().registerTypes(
				Arrays.asList(
						TypeReference.of(SimpleNeo4jRepository.class),
						TypeReference.of(SimpleQueryByExampleExecutor.class),
						TypeReference.of(CypherdslConditionExecutorImpl.class),
						TypeReference.of(BeforeBindCallback.class),
						TypeReference.of(AfterConvertCallback.class),
						// todo "temporary" fix, should get resolved when class parameters in annotations getting discovered
						TypeReference.of(UUIDStringGenerator.class),
						TypeReference.of(GeneratedValue.InternalIdGenerator.class),
						TypeReference.of(GeneratedValue.UUIDGenerator.class)
				),
				builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
						MemberCategory.INVOKE_PUBLIC_METHODS));

		if (ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)) {
			hints.reflection().registerTypes(
					Arrays.asList(
							TypeReference.of(SimpleReactiveNeo4jRepository.class),
							TypeReference.of(SimpleReactiveQueryByExampleExecutor.class),
							TypeReference.of(ReactiveCypherdslConditionExecutorImpl.class),
							TypeReference.of(ReactiveBeforeBindCallback.class)
					),
					builder -> builder.withMembers(MemberCategory.INVOKE_DECLARED_CONSTRUCTORS, MemberCategory.INVOKE_PUBLIC_METHODS));
		}

		if (QuerydslUtils.QUERY_DSL_PRESENT) {
			registerQuerydslHints(hints);
		}
	}

	private static void registerQuerydslHints(RuntimeHints hints) {

		hints.reflection().registerType(QuerydslNeo4jPredicateExecutor.class,
				MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);

		if (ReactiveWrappers.isAvailable(ReactiveWrappers.ReactiveLibrary.PROJECT_REACTOR)) {
			hints.reflection().registerType(ReactiveQuerydslNeo4jPredicateExecutor.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		}

	}
}
