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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apiguardian.api.API;

/**
 * Annotation to mark a string property as low-cardinality, which will cause
 * FalkorDB's {@code intern()} function to be applied when writing the value
 * to the database. This helps FalkorDB optimize storage by keeping only a
 * single copy of frequently repeated string values.
 * <p>
 * This annotation is useful for properties like categories, status codes,
 * country codes, or any other string field with a limited set of possible values.
 * <p>
 * Example usage:
 * <pre>
 * {@code
 * @Node("User")
 * public class User {
 *     @Id
 *     private String id;
 *     
 *     @Interned
 *     private String status; // e.g., "ACTIVE", "INACTIVE", "PENDING"
 *     
 *     @Interned
 *     private String country; // e.g., "US", "UK", "CA"
 * }
 * }
 * </pre>
 *
 * @author Shahar Biron (FalkorDB)
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.ANNOTATION_TYPE })
@Documented
@API(status = API.Status.STABLE, since = "1.0")
public @interface Interned {

}
