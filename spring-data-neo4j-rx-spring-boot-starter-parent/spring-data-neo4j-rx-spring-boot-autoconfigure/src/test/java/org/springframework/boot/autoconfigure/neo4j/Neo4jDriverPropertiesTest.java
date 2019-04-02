/*
 * Copyright (c) 2019 "Neo4j,"
 * Neo4j Sweden AB [https://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.autoconfigure.neo4j;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.*;
import static org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.ConfigProperties.LoadBalancingStrategy.*;
import static org.springframework.boot.autoconfigure.neo4j.Neo4jDriverProperties.TrustSettings.Strategy.*;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.internal.async.pool.PoolSettings;
import org.neo4j.driver.internal.retry.RetrySettings;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * @author Michael J. Simons
 */
class Neo4jDriverPropertiesTest {

	private AnnotationConfigApplicationContext context;

	@Nested
	@DisplayName("Configuration of uris")
	class URIsTest {
		@Test
		@DisplayName("…should allow empty list")
		void shouldAllowEmptyListOfURIs() {

			Neo4jDriverProperties driverProperties = load();
			assertThat(driverProperties.computeFinalListOfUris()).isEmpty();
		}

		@Test
		@DisplayName("…should allow more than one URI")
		void shouldAllowMoreThanOneURI() {

			Neo4jDriverProperties driverProperties = load(PREFIX + ".uris=bolt://localhost:7687,bolt://localhost:7688");
			assertThat(driverProperties.computeFinalListOfUris()).hasSize(2);
		}

		@Test
		@DisplayName("…not allow both single and multiple uris")
		void shouldNotAllowBothSingleAndMultipleUris() {

			Neo4jDriverProperties driverProperties = load(
				PREFIX + ".uri=bolt://localhost:7687",
				PREFIX + ".uris=bolt://localhost:7687,bolt://localhost:7688"
			);

			assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
				.isThrownBy(() -> driverProperties.computeFinalListOfUris())
				.withMessage(
					"Property spring.neo4j.uris with value 'bolt://localhost:7687,bolt://localhost:7688' is invalid: Cannot specify both single uri and list of uris.");
		}
	}

	@Nested
	@DisplayName("Configuration of authentication")
	class AuthenticationTest {

		@Test
		@DisplayName("…should not be empty by default")
		void shouldAllowEmptyListOfURIs() {

			Neo4jDriverProperties driverProperties = load();
			assertThat(driverProperties.getAuthentication()).isNotNull();
		}

		@Test
		@DisplayName("…should default to none")
		void noAuthenticationShouldWork() {

			Authentication authentication = new Authentication();
			assertThat(authentication.toInternalRepresentation()).isEqualTo(AuthTokens.none());
		}

		@Test
		@DisplayName("…should configure basic auth")
		void basicAuthShouldWork() {

			Authentication authentication = new Authentication();
			authentication.setUsername("Farin");
			authentication.setPassword("Urlaub");

			assertThat(authentication.toInternalRepresentation()).isEqualTo(AuthTokens.basic("Farin", "Urlaub"));
		}

		@Test
		@DisplayName("…should configure basic auth with realm")
		void basicAuthWithRealmShouldWork() {

			Authentication authentication = new Authentication();
			authentication.setUsername("Farin");
			authentication.setPassword("Urlaub");
			authentication.setRealm("Die Ärzte");

			assertThat(authentication.toInternalRepresentation())
				.isEqualTo(AuthTokens.basic("Farin", "Urlaub", "Die Ärzte"));
		}

		@Test
		@DisplayName("…should configure kerberos")
		void kerberosAuthShouldWork() {

			Authentication authentication = new Authentication();
			authentication.setKerberosTicket("AABBCCDDEE");

			assertThat(authentication.toInternalRepresentation()).isEqualTo(AuthTokens.kerberos("AABBCCDDEE"));
		}

		@Test
		@DisplayName("…should not allow ambiguous config")
		void ambiguousShouldNotBeAllowed() {

			Authentication authentication = new Authentication();
			authentication.setUsername("Farin");
			authentication.setKerberosTicket("AABBCCDDEE");

			assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
				.isThrownBy(() -> authentication.toInternalRepresentation())
				.withMessage(
					"Property spring.neo4j.authentication with value 'username=Farin,kerberos-ticket=AABBCCDDEE' is invalid: Cannot specify both username and kerberos ticket.");
		}
	}

	@Nested
	@DisplayName("Config properties")
	class ConfigPropertiesTest {
		@Test
		@DisplayName("…should default to drivers values")
		void shouldDefaultToDriversValues() {

			Config defaultConfig = Config.defaultConfig();

			Neo4jDriverProperties driverProperties = load();

			ConfigProperties configProperties = driverProperties.getConfig();
			assertThat(configProperties.isLogLeakedSessions()).isEqualTo(defaultConfig.logLeakedSessions());
			assertThat(configProperties.getMaxConnectionPoolSize()).isEqualTo(defaultConfig.maxConnectionPoolSize());
			assertDuration(configProperties.getIdleTimeBeforeConnectionTest(),
				defaultConfig.idleTimeBeforeConnectionTest());
			assertDuration(configProperties.getMaxConnectionLifetime(), defaultConfig.maxConnectionLifetimeMillis());
			assertDuration(configProperties.getConnectionAcquisitionTimeout(),
				defaultConfig.connectionAcquisitionTimeoutMillis());
			assertThat(configProperties.isEncrypted()).isEqualTo(defaultConfig.encrypted());
			assertThat(configProperties.getTrustSettings().getStrategy().name())
				.isEqualTo(defaultConfig.trustStrategy().strategy().name());
			assertThat(configProperties.getLoadBalancingStrategy().name())
				.isEqualTo(defaultConfig.loadBalancingStrategy().name());
			assertDuration(configProperties.getConnectionTimeout(), defaultConfig.connectionTimeoutMillis());
			assertDuration(configProperties.getMaxTransactionRetryTime(), RetrySettings.DEFAULT.maxRetryTimeMs());
			assertThat(configProperties.getServerAddressResolverClass()).isNull();
		}

		@Test
		void logLeakedSessionsSettingsShouldWork() {

			ConfigProperties configProperties;

			configProperties = new ConfigProperties();
			configProperties.setLogLeakedSessions(true);
			assertThat(configProperties.toInternalRepresentation().logLeakedSessions()).isTrue();

			configProperties = new ConfigProperties();
			configProperties.setLogLeakedSessions(false);
			assertThat(configProperties.toInternalRepresentation().logLeakedSessions()).isFalse();
		}

		@Test
		void maxConnectionPoolSizeSettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			configProperties.setMaxConnectionPoolSize(4711);
			assertThat(configProperties.toInternalRepresentation().maxConnectionPoolSize()).isEqualTo(4711);
		}

		@Test
		void idleTimeBeforeConnectionTestSettingsShouldWork() {

			ConfigProperties configProperties;

			configProperties = new ConfigProperties();
			assertThat(configProperties.toInternalRepresentation().idleTimeBeforeConnectionTest()).isEqualTo(-1);

			configProperties = new ConfigProperties();
			configProperties.setIdleTimeBeforeConnectionTest(Duration.ofSeconds(23));
			assertThat(configProperties.toInternalRepresentation().idleTimeBeforeConnectionTest()).isEqualTo(23_000);
		}

		@Test
		void connectionAcquisitionTimeoutSettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			configProperties.setConnectionAcquisitionTimeout(Duration.ofSeconds(23));
			assertThat(configProperties.toInternalRepresentation().connectionAcquisitionTimeoutMillis())
				.isEqualTo(23_000);
		}

		@Test
		void encryptedSettingsShouldWork() {

			ConfigProperties configProperties;

			configProperties = new ConfigProperties();
			configProperties.setEncrypted(true);
			assertThat(configProperties.toInternalRepresentation().encrypted()).isTrue();

			configProperties = new ConfigProperties();
			configProperties.setEncrypted(false);
			assertThat(configProperties.toInternalRepresentation().encrypted()).isFalse();
		}

		@Test
		void trustSettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			TrustSettings trustSettings = new TrustSettings();
			trustSettings.setStrategy(TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
			configProperties.setTrustSettings(trustSettings);
			assertThat(configProperties.toInternalRepresentation().trustStrategy().strategy()).isEqualTo(
				Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		}

		@Test
		void loadBalancingStrategySettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			configProperties.setLoadBalancingStrategy(ROUND_ROBIN);
			assertThat(configProperties.toInternalRepresentation().loadBalancingStrategy()).isEqualTo(
				Config.LoadBalancingStrategy.ROUND_ROBIN);
		}

		@Test
		void connectionTimeoutSettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			configProperties.setConnectionTimeout(Duration.ofSeconds(23));
			assertThat(configProperties.toInternalRepresentation().connectionTimeoutMillis()).isEqualTo(23_000);
		}

		@Test
		@Disabled("The internal driver has no means of retrieving that value back again")
		void maxTransactionRetryTimeSettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			configProperties.setMaxTransactionRetryTime(Duration.ofSeconds(23));
		}

		@Test
		void serverAddressResolverClassSettingsShouldWork() {

			ConfigProperties configProperties = new ConfigProperties();
			configProperties.setServerAddressResolverClass(TestServerAddressResolver.class);
			assertThat(configProperties.toInternalRepresentation().resolver())
				.isNotNull()
				.isInstanceOf(TestServerAddressResolver.class);
		}
	}

	@Nested
	@DisplayName("Trust settings")
	class TrustSettingsTest {

		@Test
		void trustAllCertificatesShouldWork() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(TRUST_ALL_CERTIFICATES);

			assertThat(settings.toInternalRepresentation().strategy()).isEqualTo(
				Config.TrustStrategy.Strategy.TRUST_ALL_CERTIFICATES);
		}

		@Test
		void shouldEnableHostnameVerification() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(TRUST_ALL_CERTIFICATES);
			settings.setHostnameVerificationEnabled(true);

			assertThat(settings.toInternalRepresentation().isHostnameVerificationEnabled()).isTrue();
		}

		@Test
		void trustSystemCertificatesShouldWork() {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);

			assertThat(settings.toInternalRepresentation().strategy()).isEqualTo(
				Config.TrustStrategy.Strategy.TRUST_SYSTEM_CA_SIGNED_CERTIFICATES);
		}

		@Test
		@DisplayName("…should recognize correctly configured custom certificates")
		void trustCustomCertificatesShouldWork1() throws IOException {

			File certFile = File.createTempFile("sdnrx", ".cert");

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
			settings.setCertFile(certFile);

			Config.TrustStrategy trustStrategy = settings.toInternalRepresentation();
			assertThat(trustStrategy.strategy()).isEqualTo(
				Config.TrustStrategy.Strategy.TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);
			assertThat(trustStrategy.certFile()).isEqualTo(certFile);
		}

		@Test
		@DisplayName("…should fail on custom certificates without cert file")
		void trustCustomCertificatesShouldWork2() throws IOException {

			TrustSettings settings = new TrustSettings();
			settings.setStrategy(TRUST_CUSTOM_CA_SIGNED_CERTIFICATES);

			assertThatExceptionOfType(InvalidConfigurationPropertyValueException.class)
				.isThrownBy(() -> settings.toInternalRepresentation())
				.withMessage(
					"Property spring.neo4j.config.trust-settings with value 'TRUST_CUSTOM_CA_SIGNED_CERTIFICATES' is invalid: Configured trust strategy requires a certificate file.");
		}
	}

	private Neo4jDriverProperties load(String... properties) {

		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(properties).applyTo(ctx);
		ctx.register(TestConfiguration.class);
		ctx.refresh();
		this.context = ctx;
		return this.context.getBean(Neo4jDriverProperties.class);
	}

	@AfterEach
	void closeContext() {
		if (this.context != null) {
			this.context.close();
		}
	}

	private static void assertDuration(Duration duration, long expectedValueInMillis) {
		if (expectedValueInMillis == PoolSettings.NOT_CONFIGURED) {
			assertThat(duration).isNull();
		} else {
			assertThat(duration.toMillis()).isEqualTo(expectedValueInMillis);
		}
	}

	@Configuration
	@EnableConfigurationProperties(Neo4jDriverProperties.class)
	static class TestConfiguration {

	}
}
