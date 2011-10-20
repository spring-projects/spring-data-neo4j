package org.springframework.data.neo4j.repository;

import org.springframework.data.neo4j.annotation.MapResult;
import org.springframework.data.neo4j.annotation.ResultColumn;
import org.springframework.data.neo4j.model.Group;
import org.springframework.data.neo4j.model.Person;

@MapResult
public interface MemberData {
    @ResultColumn("collect(team)")
    Iterable<Group> getTeams();

    @ResultColumn("boss")
    Person getBoss();
}
