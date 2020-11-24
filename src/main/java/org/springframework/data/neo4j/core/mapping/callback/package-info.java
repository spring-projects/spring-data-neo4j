/**
 * <!-- tag::intent[] -->
   This package contains the callback API. There are both imperative and reactive callbacks available that get invoked
   just before an entity is bound to a statement. These can be implemented by client code. For further convinience,
   both imperative and reactive auditing callbacks are available. The config package contains a registrar and an
   annotation to enable those without having to provide the beans manually.
 * <!-- end::intent[] -->
 * @author Michael J. Simons
 */
@NonNullApi
package org.springframework.data.neo4j.core.mapping.callback;

import org.springframework.lang.NonNullApi;
