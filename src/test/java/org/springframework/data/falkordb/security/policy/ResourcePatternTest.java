/*
 * Copyright (c) 2025 FalkorDB Ltd.
 */

package org.springframework.data.falkordb.security.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.data.falkordb.security.model.Privilege;
import org.springframework.data.falkordb.security.model.PrivilegeResource;
import org.springframework.data.falkordb.security.model.ResourceType;

class ResourcePatternTest {

	@Test
	void shouldParseNodePattern() {
		ResourcePattern p = ResourcePattern.parse("com.acme.Foo");
		assertThat(p.type()).isEqualTo(ResourceType.NODE);
		assertThat(p.labelOrType()).isEqualTo("com.acme.Foo");
		assertThat(p.property()).isNull();
	}

	@Test
	void shouldParsePropertyPatternUsingHashSeparator() {
		ResourcePattern p = ResourcePattern.parse("com.acme.Foo#bar");
		assertThat(p.type()).isEqualTo(ResourceType.PROPERTY);
		assertThat(p.labelOrType()).isEqualTo("com.acme.Foo");
		assertThat(p.property()).isEqualTo("bar");
	}

	@Test
	void shouldParseRelationshipPatternForUppercase() {
		ResourcePattern p = ResourcePattern.parse("KNOWS");
		assertThat(p.type()).isEqualTo(ResourceType.RELATIONSHIP);
		assertThat(p.labelOrType()).isEqualTo("KNOWS");
		assertThat(p.property()).isNull();
	}

	@Test
	void shouldBuildCanonicalResourceStrings() {
		assertThat(PrivilegeResource.toResourceString(ResourceType.NODE, "com.acme.Foo", null))
				.isEqualTo("com.acme.Foo");
		assertThat(PrivilegeResource.toResourceString(ResourceType.RELATIONSHIP, "KNOWS", null))
				.isEqualTo("KNOWS");
		assertThat(PrivilegeResource.toResourceString(ResourceType.PROPERTY, "com.acme.Foo", "bar"))
				.isEqualTo("com.acme.Foo.bar");
	}

	@Test
	void shouldPreferLegacyResourceStringWhenPresent() {
		Privilege p = new Privilege();
		p.setResource("*");
		p.setResourceType(ResourceType.NODE);
		p.setResourceLabel("com.acme.Foo");
		p.setResourceProperty(null);

		assertThat(PrivilegeResource.toResourceString(p)).isEqualTo("*");
	}

	@Test
	void shouldFallbackToTypedFieldsWhenLegacyResourceIsMissing() {
		Privilege p = new Privilege();
		p.setResource(null);
		p.setResourceType(ResourceType.PROPERTY);
		p.setResourceLabel("com.acme.Foo");
		p.setResourceProperty("bar");

		assertThat(PrivilegeResource.toResourceString(p)).isEqualTo("com.acme.Foo.bar");
	}
}
