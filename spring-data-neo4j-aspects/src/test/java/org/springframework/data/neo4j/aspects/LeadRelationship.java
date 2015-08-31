package org.springframework.data.neo4j.aspects;

import org.springframework.data.neo4j.annotation.*;
import org.springframework.data.neo4j.support.index.IndexType;

import java.util.Date;
import java.util.UUID;

/**
* @author mh
* @since 30.08.15
*/
@RelationshipEntity(type="LEAD")
public class LeadRelationship {

    @GraphId
    private Long id;

    @Indexed(unique = true)
    private Long uuid;

    @Indexed(indexType = IndexType.SIMPLE, indexName = "date-index")
    private Date createdDate = new Date();

    @StartNode
    private Person person;
    @EndNode
    private Group group;

    public LeadRelationship() {
    }

    public LeadRelationship(Person person, Group group) {
        this.person = person;
        this.group = group;
        this.uuid = Math.abs(UUID.randomUUID().getMostSignificantBits());
    }

    public Long getId() {
        return id;
    }

    public Long getUuid() {
        return uuid;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Person getPerson() {
        return person;
    }

    public Group getGroup() {
        return group;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }
}
