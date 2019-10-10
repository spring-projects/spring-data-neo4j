/**
 * Contains a test slice for SDN/RX, providing an embedded Neo4j instance if the Neo4j Test-Harness is available on the
 * classpath. Otherwise delegates to the auto configuration of the Bolt driver for a usable driver bean.
 */
package org.neo4j.springframework.boot.test.autoconfigure.data;
