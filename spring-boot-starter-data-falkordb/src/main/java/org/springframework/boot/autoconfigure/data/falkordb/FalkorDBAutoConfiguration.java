package org.springframework.boot.autoconfigure.data.falkordb;

import com.falkordb.Driver;
import com.falkordb.impl.api.DriverImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.falkordb.core.DefaultFalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBClient;
import org.springframework.data.falkordb.core.FalkorDBTemplate;
import org.springframework.data.falkordb.core.query.FalkorDBQueryRewriter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBEntityConverter;
import org.springframework.data.falkordb.core.mapping.DefaultFalkorDBMappingContext;
import org.springframework.data.falkordb.core.mapping.FalkorDBMappingContext;
import org.springframework.data.mapping.model.EntityInstantiators;

/**
 * Auto-configuration for Spring Data FalkorDB.
 *
 * @author Shahar Biron
 * @since 1.0
 */
@AutoConfiguration
@ConditionalOnClass({Driver.class, FalkorDBTemplate.class})
@EnableConfigurationProperties(FalkorDBProperties.class)
public class FalkorDBAutoConfiguration {

	/**
	 * Creates a FalkorDB driver bean.
	 * @param properties the FalkorDB properties
	 * @return configured FalkorDB driver
	 */
	@Bean
	@ConditionalOnMissingBean
	public Driver falkorDBDriver(FalkorDBProperties properties) {
		String uri = properties.getUri();
		
		// Parse URI and create FalkorDB connection
		// URI format: falkordb://host:port or redis://host:port
		String host = "localhost";
		int port = 6379;
		
		if (uri != null && !uri.isEmpty()) {
			uri = uri.replace("falkordb://", "").replace("redis://", "");
			String[] parts = uri.split(":");
			if (parts.length > 0) {
				host = parts[0];
			}
			if (parts.length > 1) {
				try {
					port = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e) {
					// Keep default port
				}
			}
		}

		return new DriverImpl(host, port);
	}

	/**
	 * Creates a FalkorDB client bean.
	 * Requires spring.data.falkordb.database to be configured.
	 * @param driver the FalkorDB driver
	 * @param properties the FalkorDB properties
	 * @return configured FalkorDB client
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnProperty(prefix = "spring.data.falkordb", name = "database")
	public FalkorDBClient falkorDBClient(Driver driver, FalkorDBProperties properties) {
		return new DefaultFalkorDBClient(driver, properties.getDatabase());
	}

	/**
	 * Creates a FalkorDB mapping context bean.
	 * @return FalkorDB mapping context
	 */
	@Bean
	@ConditionalOnMissingBean
	public FalkorDBMappingContext falkorDBMappingContext() {
		return new DefaultFalkorDBMappingContext();
	}

	/**
	 * Creates a FalkorDB template bean.
	 * Only created if FalkorDBClient is available.
	 * @param client the FalkorDB client
	 * @param mappingContext the mapping context
	 * @return FalkorDB template
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnBean(FalkorDBClient.class)
	public FalkorDBTemplate falkorDBTemplate(FalkorDBClient client,
			FalkorDBMappingContext mappingContext,
			ObjectProvider<FalkorDBQueryRewriter> queryRewriterProvider) {
		DefaultFalkorDBEntityConverter converter = new DefaultFalkorDBEntityConverter(
				mappingContext, new EntityInstantiators(), client);
		return new FalkorDBTemplate(client, mappingContext, converter, queryRewriterProvider.getIfAvailable());
	}
}
