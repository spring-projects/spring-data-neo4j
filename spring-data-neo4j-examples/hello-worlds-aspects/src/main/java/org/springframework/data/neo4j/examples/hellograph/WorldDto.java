package org.springframework.data.neo4j.examples.hellograph;

public class WorldDto {
	private final Long id;
	private final String name;
	private final int moons;
	
	public WorldDto(Long id, String name, int moons) {		
		this.id = id;
		this.name = name;
		this.moons = moons;
	}
	
	public Long getId() {
		return id;
	}
	
	public String getName() {
		return name;
	}
	
	public int getMoons() {
		return moons;
	}
	
	@Override
    public String toString() {
        return String.format("WorldDto{id=%d, name='%s', moons=%d}", id, name, moons);
    }
}