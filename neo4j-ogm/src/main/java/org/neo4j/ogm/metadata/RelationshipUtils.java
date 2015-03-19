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

/**
 * Contains helper methods to facilitate inference of relationship types from field and methods and vice versa.
 * <p>
 * All methods follow the same convention that relationship types are UPPER_SNAKE_CASE and that fields appear in
 * lowerCamelCase.  The de-facto Java Bean getter/setter pattern is also assumed when inferring accessor methods.
 * </p>
 * The utility methods here will all throw a <code>NullPointerException</code> if invoked with <code>null</code>.
 *
 * @author Adam George
 */
public class RelationshipUtils {

    /**
     * Infers the relationship type that corresponds to the given field or access method name.
     * This method is called when no annotation exists by which to determine the relationship
     * type between two nodes.
     *
     * @param memberName The member name from which to infer the relationship type
     * @return The resolved relationship type
     */
    public static String inferRelationshipType(String memberName) {
        if (memberName.startsWith("get") || memberName.startsWith("set")) {
            return toUpperSnakeCase(memberName.substring(3)).toString();
        }
        return toUpperSnakeCase(memberName).toString();
    }

    /**
     * Infers the name of the setter method that corresponds to the given relationship type.
     *
     * @param relationshipType The relationship type from which to infer the setter name
     * @return The inferred setter method name
     */
    public static String inferSetterName(String relationshipType) {
        StringBuilder setterName = toQuasiCamelCase(new StringBuilder("set"), relationshipType);
        return setterName.toString();
    }

    /**
     * Infers the name of the getter method that corresponds to the given relationship type.
     *
     * @param relationshipType The relationship type from which to infer the getter name
     * @return The inferred getter method name
     */
    public static String inferGetterName(String relationshipType) {
        StringBuilder getterName = toQuasiCamelCase(new StringBuilder("get"), relationshipType);
        return getterName.toString();
    }

    /**
     * Infers the name of the instance variable that corresponds to the given relationship type.
     *
     * @param relationshipType The relationship type from which to infer the name of the field
     * @return The inferred field name
     */
    public static String inferFieldName(String relationshipType) {
        StringBuilder fieldName = toQuasiCamelCase(new StringBuilder(), relationshipType);
        fieldName.setCharAt(0, Character.toLowerCase(fieldName.charAt(0)));
        return fieldName.toString();
    }

    /**
     * Converts a String, possibly containing the character '_' to QuasiCamelCase
     * and appends the converted String to the provided StringBuilder.
     *
     * Example: SNAKE_CASE -> SnakeCase
     *
     * Note that the first character of the converted String is in uppercase.
     * This is intentional and should not be changed, because other parts of
     * the code expect this format in order to operate correctly.
     *
     * @param sb The StringBuilder object which will hold the converted string
     * @param name the string Value to convert.
     * @return
     */
    private static StringBuilder toQuasiCamelCase(StringBuilder sb, String name) {
        if (name != null && name.length() > 0) {
            if (!name.contains("_")) {
                sb.append(name.substring(0, 1).toUpperCase());
                sb.append(name.substring(1).toLowerCase());
            } else {
                String[] parts = name.split("_");
                for (String part : parts) {
                    String test = part.toLowerCase();
                    toQuasiCamelCase(sb, test);
                }
            }
        }
        return sb;
    }

    /**
     * Converts a String to UPPER_SNAKE_CASE
     * and appends the converted String to the provided StringBuilder.
     *
     * Example: snakeCase -> SNAKE_CASE
     *
     * This method is the dual of toQuasiCamelCase, meaning
     *
     * toQuasiCamelCase(toUpperSnakeCase("SnakeCase")) will return "SnakeCase"
     *
     * and
     *
     * toUpperSnakeCase(toUpperCamelCase("SNAKE_CASE")) will return "SNAKE_CASE"
     *
     * @param name the string Value to convert.
     * @return
     */
    private static StringBuilder toUpperSnakeCase(String name) {
        StringBuilder sb = new StringBuilder();
        if (name != null && name.length() > 0) {
            for (Character ch : name.toCharArray()) {
                if (Character.isLowerCase(ch)) {
                    ch = Character.toUpperCase(ch);
                } else {
                    if (sb.length() > 0) {
                        sb.append("_");
                    }
                }
                sb.append(ch);
            }
        }
        return sb;
    }
}
