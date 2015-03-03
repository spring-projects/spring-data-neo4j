/*
 * Copyright (c) 2014-2015 "GraphAware"
 *
 * GraphAware Ltd
 *
 * This file is part of Neo4j-OGM.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
