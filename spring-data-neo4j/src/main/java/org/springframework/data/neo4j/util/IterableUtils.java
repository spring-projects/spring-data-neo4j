/*
 * Copyright (c)  [2011-2016] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 *
 */

package org.springframework.data.neo4j.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.neo4j.ogm.exception.core.NotFoundException;

/**
 * Utility methods for dealing with {@link Iterable}s.
 *
 * @author Michal Bachman
 */
public final class IterableUtils {

	/**
	 * Count items in an iterable.
	 *
	 * @param iterable to count items in.
	 * @return number of items in the iterable.
	 */
	public static long count(Iterable iterable) {
		if (iterable instanceof Collection) {
			return ((Collection) iterable).size();
		}

		int count = 0;
		for (Object o : iterable) {
			count++;
		}

		return count;
	}

	/**
	 * Check whether an iterable contains the given object.
	 *
	 * @param iterable to check in.
	 * @param object to look for.
	 * @param <T> type of the objects stored in the iterable.
	 * @return true iff the object is contained in the iterable.
	 */
	public static <T> boolean contains(Iterable<T> iterable, T object) {
		if (iterable instanceof Collection) {
			return ((Collection) iterable).contains(object);
		}

		for (T t : iterable) {
			if (t.equals(object)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Convert an iterable to a list.
	 *
	 * @param iterable to convert.
	 * @param <T> type of the items held.
	 * @return a list.
	 */
	public static <T> List<T> toList(Iterable<T> iterable) {
		List<T> list = new ArrayList<>();

		if (iterable instanceof Collection) {
			list.addAll((Collection<T>) iterable);
		} else {
			for (T next : iterable) {
				list.add(next);
			}
		}

		return list;
	}

	/**
	 * Get a single element from iterator.
	 *
	 * @param iterator to find a single element.
	 * @param notFoundMessage exception message if there are no elements.
	 * @param <T> type of the element.
	 * @return the element iff there is exactly one.
	 * @throws NotFoundException in case there are no elements.
	 * @throws IllegalStateException in case the iterable contains more than 1 element.
	 */
	public static <T> T getSingle(Iterator<T> iterator, String notFoundMessage) {
		T result = getSingleOrNull(iterator);

		if (result == null) {
			throw new NotFoundException(notFoundMessage);
		}

		return result;
	}

	/**
	 * Get a single element from iterable.
	 *
	 * @param iterable to find a single element.
	 * @param notFoundMessage exception message if there are no elements.
	 * @param <T> type of the element.
	 * @return the element iff there is exactly one.
	 * @throws NotFoundException in case there are no elements.
	 * @throws IllegalStateException in case the iterable contains more than 1 element.
	 */
	public static <T> T getSingle(Iterable<T> iterable, String notFoundMessage) {
		return getSingle(iterable.iterator(), notFoundMessage);
	}

	/**
	 * Get a single element from iterator.
	 *
	 * @param iterator to find a single element.
	 * @param <T> type of the element.
	 * @return the element iff there is exactly one.
	 * @throws NotFoundException in case there are no elements.
	 * @throws IllegalStateException in case the iterable contains more than 1 element.
	 */
	public static <T> T getSingle(Iterator<T> iterator) {
		return getSingle(iterator, "Iterator is empty");
	}

	/**
	 * Get a single element from iterable.
	 *
	 * @param iterable to find a single element.
	 * @param <T> type of the element.
	 * @return the element iff there is exactly one.
	 * @throws NotFoundException in case there are no elements.
	 * @throws IllegalStateException in case the iterable contains more than 1 element.
	 */
	public static <T> T getSingle(Iterable<T> iterable) {
		return getSingle(iterable.iterator(), "Iterable is empty");
	}

	/**
	 * Get a single element from iterator.
	 *
	 * @param iterator to find a single element.
	 * @param <T> type of the element.
	 * @return the element iff there is exactly one, null iff there is 0.
	 * @throws IllegalStateException in case the iterable contains more than 1 element.
	 */
	public static <T> T getSingleOrNull(Iterator<T> iterator) {
		T result = null;

		if (iterator.hasNext()) {
			result = iterator.next();
		}

		if (iterator.hasNext()) {
			throw new IllegalStateException("Iterable has more than one element, which is unexpected");
		}

		return result;
	}

	/**
	 * Get a single element from iterable.
	 *
	 * @param iterable to find a single element.
	 * @param <T> type of the element.
	 * @return the element iff there is exactly one, null iff there is 0.
	 * @throws IllegalStateException in case the iterable contains more than 1 element.
	 */
	public static <T> T getSingleOrNull(Iterable<T> iterable) {
		return getSingleOrNull(iterable.iterator());
	}

	/**
	 * Get the first element from iterator.
	 *
	 * @param iterator to find the first element.
	 * @param notFoundMessage exception message if there are no elements.
	 * @param <T> type of the element.
	 * @return the element iff there is one or more.
	 * @throws NotFoundException in case there are no elements.
	 */
	public static <T> T getFirst(Iterator<T> iterator, String notFoundMessage) {
		T result = null;

		if (iterator.hasNext()) {
			result = iterator.next();
		}

		if (result == null) {
			throw new NotFoundException(notFoundMessage);
		}

		return result;
	}

	/**
	 * Get the first element from iterable.
	 *
	 * @param iterable to find the first element.
	 * @param notFoundMessage exception message if there are no elements.
	 * @param <T> type of the element.
	 * @return the element iff there is one or more.
	 * @throws NotFoundException in case there are no elements.
	 */
	public static <T> T getFirst(Iterable<T> iterable, String notFoundMessage) {
		return getFirst(iterable.iterator(), notFoundMessage);
	}

	/**
	 * Get the first element from iterator.
	 *
	 * @param iterator to find the first element.
	 * @param <T> type of the element.
	 * @return the element iff there is one or more, null if there is none.
	 */
	public static <T> T getFirstOrNull(Iterator<T> iterator) {
		T result = null;

		if (iterator.hasNext()) {
			result = iterator.next();
		}

		return result;
	}

	/**
	 * Get the first element from iterable.
	 *
	 * @param iterable to find the first element.
	 * @param <T> type of the element.
	 * @return the element iff there is one or more, null if there is none.
	 */
	public static <T> T getFirstOrNull(Iterable<T> iterable) {
		return getFirstOrNull(iterable.iterator());
	}

	/**
	 * private constructor to prevent instantiation.
	 */
	private IterableUtils() {}
}
