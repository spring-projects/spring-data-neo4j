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
 * Declares row-level security filter metadata on an entity.
 *
 * MVP: the filter is a simple expression string that can be interpreted
 * by higher layers. The core repository will treat this mostly as a marker
 * until a query rewriting layer is introduced.
 */
@Documented
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@API(status = API.Status.EXPERIMENTAL, since = "1.0")
public @interface RowLevelSecurity {

	/**
	 * Expression describing the row filter.
	 */
	String filter();

}
