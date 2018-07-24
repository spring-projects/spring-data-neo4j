/*
 * Copyright (c)  [2011-2017] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.neo4j.repository.config.Neo4jAuditingRegistrar;

/**
 * Annotation to enable auditing in MongoDB via annotation configuration.
 *
 * @author Frantisek Hartman
 */
@Inherited
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(Neo4jAuditingRegistrar.class)
public @interface EnableNeo4jAuditing {

	/**
	 * Configures the {@link AuditorAware} bean to be used to lookup the current principal.
	 *
	 * @return
	 */
	String auditorAwareRef() default "";

	/**
	 * Configures whether the creation and modification dates are set. Defaults to {@literal true}.
	 *
	 * @return
	 */
	boolean setDates() default true;

	/**
	 * Configures whether the entity shall be marked as modified on creation. Defaults to {@literal true}.
	 *
	 * @return
	 */
	boolean modifyOnCreate() default true;

	/**
	 * Configures a {@link DateTimeProvider} bean name that allows customizing the {@link org.joda.time.DateTime} to be
	 * used for setting creation and modification dates.
	 *
	 * @return
	 */
	String dateTimeProviderRef() default "";
}
