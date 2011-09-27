package org.springframework.data.neo4j.examples.hellograph;

import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * @author mh
 * @since 01.04.11
 */
public interface MyWorldRepository {
    @Transactional
    Collection<World> makeSomeWorlds();

    @Transactional
    World world(String name, int moons);

    World findWorldNamed(String name);

    Iterable<World> findWorldsWithMoons(int moonCount);

    Iterable<World> exploreWorldsBeyond(World homeWorld);
}
