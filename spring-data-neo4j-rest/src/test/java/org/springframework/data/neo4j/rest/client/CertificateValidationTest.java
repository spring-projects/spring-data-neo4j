/*
 * Copyright 2014,2015,2016 Vince Bickers
 *
 * This file is part of Neo4j-Databridge.
 *
 * Neo4j-Databridge is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.springframework.data.neo4j.rest.client;

import com.sun.jersey.api.client.ClientHandlerException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.neo4j.rest.graphdb.ExecutingRestRequest;
import org.neo4j.rest.graphdb.RequestResult;
import org.neo4j.rest.graphdb.util.Config;

/**
 * By default, you cannot make a secure connection to a Neo4j instance
 * unless you have a valid certificate installed.
 *
 * In some cases, installing a certificate may not be possible, so a Configuration
 * property can be set to ignore SSL handshaking.
 *
 * Please note: these tests require an external Neo4j server to be running,
 * authenticating connections with 'neo4j:password'.
 *
 * So they are @Ignored by default.
 *
 * @author vince
 */
@Ignore
public class CertificateValidationTest {

    @Before
    public void setUp() {
        System.setProperty(Config.IGNORE_SSL_HANDSHAKE, "false"); // this is the default behaviour
    }

    @Test
    public void shouldEnableCertificateValidationByDefault() {
        Assert.assertFalse(Config.ignoreSSLHandshake());
    }

    /**
     * Note: the mechanism to ignore SSL handshaking installs a trust-everybody trust manager into
     * the JVM. There is no mechanism to restore the previous trust manager while he JVM is running
     * Consequently, this test cannot be split up because we can't guarantee the order in which
     * the different parts would run, whereas in a single test, we're in control.
     */
    @Test
    public void shouldDisableCertificateValidationIfIgnoreSSLHandshake() {

        Assert.assertFalse(Config.ignoreSSLHandshake());

        try {
            request().get("");
            Assert.fail("Should have thrown security exception");
        } catch (ClientHandlerException clientHandlerException) {
            // expected
        }

        // now set the config to ignore SSL handshaking and try again;
        System.setProperty(Config.IGNORE_SSL_HANDSHAKE, "true");
        Assert.assertTrue(Config.ignoreSSLHandshake());

        try {
            RequestResult result = request().get("");
            Assert.assertEquals(200, result.getStatus());
            Assert.assertEquals("{\"management\":\"https://localhost:7473/db/manage/\",\"data\":\"https://localhost:7473/db/data/\"}", result.getText());
        } catch (Exception e) {
            Assert.fail("Should not have thrown exception: " + e);
        }

    }

    private ExecutingRestRequest request() {
        return new ExecutingRestRequest("https://localhost:7473", "neo4j", "password");
    }

}
