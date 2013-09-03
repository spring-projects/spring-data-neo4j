package org.springframework.data.neo4j.model;

/**
 * A concrete version (1) of AbstractNodeEntity
 */
public class Concrete1NodeEntity extends AbstractNodeEntity {

    public Concrete1NodeEntity() {
        super();
    }

    public Concrete1NodeEntity(String name) {
       super(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Concrete1NodeEntity otherNode = (Concrete1NodeEntity) o;
        if (id == null) return super.equals(o);
        return id.equals(otherNode.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : super.hashCode();
    }
}