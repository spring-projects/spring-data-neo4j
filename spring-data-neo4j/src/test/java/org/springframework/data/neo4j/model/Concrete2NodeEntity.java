package org.springframework.data.neo4j.model;

/**
 * A concrete version (2) of AbstractNodeEntity
 */
public class Concrete2NodeEntity extends AbstractNodeEntity {

    public Concrete2NodeEntity() {
        super();
    }

    public Concrete2NodeEntity(String name) {
       super(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Concrete2NodeEntity otherNode = (Concrete2NodeEntity) o;
        if (id == null) return super.equals(o);
        return id.equals(otherNode.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }

}