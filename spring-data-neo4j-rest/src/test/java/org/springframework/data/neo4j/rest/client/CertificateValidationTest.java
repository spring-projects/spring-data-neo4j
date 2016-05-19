/**
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
