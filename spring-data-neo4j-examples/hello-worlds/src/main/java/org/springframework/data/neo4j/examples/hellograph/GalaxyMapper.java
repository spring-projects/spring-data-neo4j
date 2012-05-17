package org.springframework.data.neo4j.examples.hellograph;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class GalaxyMapper {
	public WorldDto worldDtoFromWorld(World world) {
		if(world == null) {
			return null;
		}
		
		return new WorldDto(world.getId(), world.getName(), world.getMoons());
	}
	
	public List<WorldDto> worldDtosFromWorlds(Iterable<World> worlds) {
		List<WorldDto> worldDtos = new ArrayList<WorldDto>();
		for(World world : worlds) {
			WorldDto worldDto = worldDtoFromWorld(world);
			worldDtos.add(worldDto);
		}
		
		return worldDtos;
	}
}