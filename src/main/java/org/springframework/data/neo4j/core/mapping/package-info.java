/**
 * <!-- tag::intent[] -->
   The main mapping framework. This package orchestrates the reading and writing of entities and all tasks related to it.
   The only public API of this package is the subpackage {@literal callback}, containing the event support.
   The core package itself has to be considered an internal api and we don't give any guarantees of API stability.
 * <!-- end::intent[] -->
 * @author Michael J. Simons
 */
@NonNullApi
package org.springframework.data.neo4j.core.mapping;

import org.springframework.lang.NonNullApi;
