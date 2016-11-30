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
package org.springframework.data.neo4j.extensions;

import java.io.Serializable;

import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * This interface declares a custom method that we want to
 * be available to all repositories that extend this class
 *
 * @author: Vince Bickers
 */
@NoRepositoryBean
public interface CustomGraphRepository<T, ID extends Serializable> extends GraphRepository<T, ID> {

    boolean sharedCustomMethod();

}
