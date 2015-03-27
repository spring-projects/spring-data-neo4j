unit.mapper.transitive

tests relationship operations where two node-backed entities are transitively connected (i.e. via a RelationshipEntity).

tests fall into three distinct categories

type            package
----            -------
one to one      unit.mapper.transitive.ab
one to many     unit.mapper.transitive.abb
many to many    unit.mapper.transitive.aabb

the non-transitive analogue of these tests is in unit.mapper.direct

creating a new test
-------------------
if you create a test in any of these packages, you must make a corresponding test in all of the others,
where appropriate. That means you will have up to six versions of the same test, configured for each of the
different entity models.
