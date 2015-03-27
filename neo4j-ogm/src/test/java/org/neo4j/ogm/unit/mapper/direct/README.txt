unit.mapper.direct

tests relationship operations where two node-backed entities are related directly (i.e. not using a RelationshipEntity).

tests fall into three distinct categories

type            package
----            -------
one to one      unit.mapper.direct.ab
one to many     unit.mapper.direct.abb
many to many    unit.mapper.direct.aabb

the transitive analogue of these tests is in unit.mapper.transitive

creating a new test
-------------------
if you create a test in any of these packages, you must make a corresponding test in all of the others,
where appropriate. That means you will have up to six versions of the same test, configured for each of the
different entity models.

