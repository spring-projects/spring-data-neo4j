/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Declares RBAC metadata on an aggregate root or entity type.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public @interface Secured {

	/**
	 * Roles that can read instances of this type.
	 */
	String[] read() default {};

	/**
	 * Roles that can write (update) instances of this type.
	 */
	String[] write() default {};

	/**
	 * Roles that can create instances of this type.
	 */
	String[] create() default {};

	/**
	 * Roles that can delete instances of this type.
	 */
	String[] delete() default {};

	/**
	 * Denied properties for read operations.
	 */
	DenyProperty[] denyReadProperties() default {};

	/**
	 * Denied properties for write operations.
	 */
	DenyProperty[] denyWriteProperties() default {};

}
