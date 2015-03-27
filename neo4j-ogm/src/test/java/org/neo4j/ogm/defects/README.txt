About these tests
-----------------
The tests in the org.neo4j.ogm.defects package are all expected to fail!

These tests do not get run as part of the ordinary build, but are here to document bugs in our code.
Ideally, therefore every test here should have a referring entry in JIRA.

As defects are fixed, the corresponding test(s) in this package should be moved into the appropriate package
(unit or integration).

To run the defects test-suite from maven, use the 'defects' profile:

mvn clean test -Pdefects



