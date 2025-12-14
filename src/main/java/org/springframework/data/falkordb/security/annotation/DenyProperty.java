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
 * Declares a property that should be denied for specific roles.
 */
@Documented
@Target({ ElementType.ANNOTATION_TYPE })
@Retention(RetentionPolicy.RUNTIME)
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public @interface DenyProperty {

	/**
	 * Name of the property on the domain type.
	 */
	String property();

	/**
	 * Roles for which this property is denied.
	 */
	String[] forRoles() default {};

}
