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

        galaxy.makeSomeWorlds();

        World homeWorld = galaxy.findWorldNamed( "Earth" );
        System.out.println( "At home on: " + homeWorld );

        Iterable<World> worldsBeyond = galaxy.exploreWorldsBeyond( homeWorld );
        for (World world : worldsBeyond) {
            System.out.println( "found worlds beyond: " + world );
        }

        applicationContext.close();
        
    }

}
