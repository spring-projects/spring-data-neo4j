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

package org.springframework.data.neo4j.server;

import org.neo4j.ogm.driver.Driver;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Vince Bickers
 * @author Luanne Misquitta
 */
public class RemoteServer implements Neo4jServer {

    private String url;
    private String username;
    private String password;

    public RemoteServer(Driver driver) {
        this.url = driver.getConfiguration().getURI();
        //extractAuth(url);
        throw new RuntimeException("Not working yet, what's the point??");
    }

    private void extractAuth(String url) {
        try {
           URL u = new URL(url);
           String userInfo = u.getUserInfo();
           if (userInfo != null) { 
              this.username = userInfo.split(":")[0];
              this.password = userInfo.split(":")[1];
           } 
        } catch(MalformedURLException e) {
             throw new IllegalArgumentException("Invalid URL "+url,e);
        }
    }

    public RemoteServer(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String url() {
        return this.url;
    }

    @Override
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

}