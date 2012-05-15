package org.springframework.data.neo4j.examples.hellograph;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class App {
    public static void main(String[] args) {
    	ConfigurableApplicationContext applicationContext =  
        	new ClassPathXmlApplicationContext("/spring/helloWorldContext.xml");

    	MyWorldRepository galaxy = applicationContext.getBean(WorldRepositoryImpl.class);
        galaxy.makeSureGalaxyIsNotEmpty();
         
        System.out.println("Trying to find the Earth by its name:");
        World earth = galaxy.findWorldNamed("Earth");
        System.out.printf("Found Earth: %s\n", earth);

        System.out.println("Retrieveing the list of worlds that can be reached from the Earth:");
        for(World world : earth.getReachableWorlds()) {
            System.out.printf("Can travel between %s and %s\n", earth, world);
        }
        
        System.out.println("Here's the list of all worlds in the galaxy:");
        for(World world : galaxy.findAllWorlds()) {
        	System.out.printf("There's a world: %s\n", world);
        }

        applicationContext.close();
    }
}
