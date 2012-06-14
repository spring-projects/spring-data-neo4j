package org.springframework.data.neo4j.annotation;

public abstract class IdentifiableEntity {
    @GraphId
    private Long id;

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals( Object obj ) {
        return obj instanceof IdentifiableEntity && id.equals( ((IdentifiableEntity) obj).getId() );
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
