/**
 * <!-- tag::intent[] -->
   Contains the core infrastructure for translating unmanaged Neo4j transaction into Spring managed transactions. Exposes
   both the imperative and reactive `TransactionManager` as `Neo4jTransactionManager` and `ReactiveNeo4jTransactionManager`.
 * <!-- end::intent[] -->
 */
@NonNullApi
package org.springframework.data.neo4j.core.transaction;

import org.springframework.lang.NonNullApi;
