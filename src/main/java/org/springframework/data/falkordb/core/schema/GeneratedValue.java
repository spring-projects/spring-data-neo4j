/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.core.schema;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Annotation for configuring generated values for entity IDs in FalkorDB. This annotation
 * can be used with different ID generation strategies.
 *
 * <p>
 * For internally generated IDs (FalkorDB's internal ID mechanism): <pre>
 * &#64;Id &#64;GeneratedValue
 * private Long id;
 * </pre>
 *
 * <p>
 * For custom ID generators: <pre>
 * &#64;Id &#64;GeneratedValue(UUIDStringGenerator.class)
 * private String uuid;
 * </pre>
 *
 * @author Michael J. Simons
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
@Inherited
@API(status = API.Status.STABLE, since = "1.0")
public @interface GeneratedValue {

	/**
	 * The class of the ID generator to use. If not specified, the framework will use
	 * FalkorDB's internal ID generation for Long/long types.
	 * @return the generator class
	 */
	Class<? extends IdGenerator<?>> generatorClass() default InternalIdGenerator.class;

	/**
	 * Optional reference name for the generator instance.
	 * @return the generator reference
	 */
	String generatorRef() default "";

}
