About these tests
-----------------
The tests in the org.neo4j.ogm.server package use a real Neo4j server instance running in a separate JVM on localhost:7474

This is because some tests require an actual server instead of a in-memory instance - for example, the 2.2 auth tests.


Authentication tests
--------------------
To run server tests from maven, you need to do two things:

1. ensure your 2.2 neo4j instance is running and configured to authenticate as follows:

    username "neo4j"
    password "password"

2. run the tests using the 2.2 profile:

mvn clean test -P2.2

