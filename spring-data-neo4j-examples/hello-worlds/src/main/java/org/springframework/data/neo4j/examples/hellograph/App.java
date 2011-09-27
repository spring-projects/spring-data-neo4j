package org.springframework.data.neo4j.examples.hellograph;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Hello world(s)!
 * <p/>
 * An example application for exploring Spring Data Neo4j.
 */
public class App
{

    public static void main( String[] args )
    {
    	
        ConfigurableApplicationContext applicationContext =  
        	new ClassPathXmlApplicationContext( "/spring/helloWorldContext.xml");

        WorldRepositoryImpl galaxy = applicationContext.getBean(WorldRepositoryImpl.class);

        Iterable<World> worlds = galaxy.makeSomeWorlds();

        World homeWorld = worlds.iterator().next();
        System.out.println("At home on: " + homeWorld);

        World foundHomeWorld = galaxy.findWorldNamed( homeWorld.getName() );
        System.out.println( "found home world: " + foundHomeWorld );

        Iterable<World> worldsBeyond = galaxy.exploreWorldsBeyond( homeWorld );
        for (World world : worldsBeyond) {
            System.out.println( "found worlds beyond: " + world );
        }

        applicationContext.close();
        
    }

}
