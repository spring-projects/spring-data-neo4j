package org.springframework.data.neo4j.examples.hellograph;

import org.springframework.transaction.annotation.Transactional;

public interface MyWorldRepository {
    @Transactional
    void makeSureGalaxyIsNotEmpty();
    
    World findWorldNamed(String name);
    
    Iterable<World> findAllWorlds();
}
