/*
 * Copyright (c)  [2011-2015] "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.neo4j.ogm.metadata;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Vince Bickers
 */
public abstract class ClassUtils {

    @SuppressWarnings("serial")
    private static final Map<String, Class<?>> PRIMITIVE_TYPE_MAP = new HashMap<String, Class<?>>() {{
        put("Z", Boolean.TYPE);
        put("B", Byte.TYPE);
        put("C", Character.TYPE);
        put("D", Double.TYPE);
        put("F", Float.TYPE);
        put("I", Integer.TYPE);
        put("J", Long.TYPE);
        put("S", Short.TYPE);
    }};

    /**
     * Return the reified class for the parameter of a JavaBean setter from the setter signature
     */
    public static Class<?> getType(String setterDescriptor) {

        int p = setterDescriptor.indexOf("(");
        int q = setterDescriptor.indexOf(")");

        if (!setterDescriptor.contains("[")) {
            if (setterDescriptor.endsWith(";)V")) {
                q--;
            }
            if (setterDescriptor.startsWith("(L")) {
                p++;
            }
        }
        String typeName = setterDescriptor.substring(p + 1, q).replace("/", ".");
        if (typeName.length() == 1) {
            return PRIMITIVE_TYPE_MAP.get(typeName);
        }

        try {
            return Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a list of unique elements on the classpath as File objects, preserving order.
     * Classpath elements that do not exist are not returned.
     * @param classPaths classpaths to be included
     */
    public static ArrayList<File> getUniqueClasspathElements(List<String> classPaths) {
        ArrayList<File> pathFiles = new ArrayList<>();
        for(String classPath : classPaths) {
            try {
                Enumeration<URL> resources = ClassUtils.class.getClassLoader().getResources(classPath.replace(".","/"));
                while(resources.hasMoreElements()) {
                    URL resource = resources.nextElement();
                    if(resource.getProtocol().equals("file")) {
                        pathFiles.add(new File(resource.toURI()));
                    }
                    else if(resource.getProtocol().equals("jar")) {
                        String jarPath = resource.getPath().substring(5, resource.getPath().indexOf("!"));  //Strip out the jar protocol
                        pathFiles.add(new File(jarPath));

                    }
                }
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return pathFiles;
    }

}
