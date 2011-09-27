package org.springframework.data.neo4j.examples.hellograph;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mh
 * @since 17.02.11
 */
public class WorldCounter {

    public Map<String, Integer> countMoons(Iterable<World> worlds) {
        Map<String, Integer> moons = new HashMap<String, Integer>();
        for (World world : worlds) {
            moons.put(world.getName(), world.getMoons());
        }
        return moons;
    }

}
