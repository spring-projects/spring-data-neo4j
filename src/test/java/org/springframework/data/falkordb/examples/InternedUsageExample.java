/*
 * Copyright (c) 2023-2025 FalkorDB Ltd.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.springframework.data.falkordb.examples;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.Interned;
import org.springframework.data.falkordb.core.schema.Node;

/**
 * Example demonstrating the use of {@link Interned} annotation for low-cardinality
 * string properties.
 *
 * <p>The {@code @Interned} annotation applies FalkorDB's intern() function to string
 * properties, which optimizes storage by keeping only a single copy of frequently
 * repeated string values.</p>
 *
 * <p>This is particularly useful for properties with a limited set of possible values
 * (low cardinality), such as:</p>
 * <ul>
 *   <li>Status codes (ACTIVE, INACTIVE, PENDING)</li>
 *   <li>Country or region codes (US, UK, CA)</li>
 *   <li>Categories (SPORTS, NEWS, TECH)</li>
 *   <li>Priorities (HIGH, MEDIUM, LOW)</li>
 *   <li>Any enum-like string values</li>
 * </ul>
 *
 * @author Shahar Biron (FalkorDB)
 * @since 1.0
 */
public class InternedUsageExample {

	/**
	 * Example entity representing a user with various low-cardinality properties.
	 */
	@Node("User")
	public static class User {
		
		@Id
		@GeneratedValue
		private Long id;
		
		private String name;
		private String email;
		
		/**
		 * Account status - typically has limited values like "ACTIVE", "INACTIVE", 
		 * "SUSPENDED", "PENDING_VERIFICATION".
		 * Using @Interned saves memory when thousands of users share the same status.
		 */
		@Interned
		private String accountStatus;
		
		/**
		 * User role - typically has limited values like "ADMIN", "USER", "MODERATOR".
		 * Perfect candidate for intern() as roles are reused across many users.
		 */
		@Interned
		private String role;
		
		/**
		 * Country code - ISO country codes are limited to about 250 values.
		 * Excellent use case for @Interned.
		 */
		@Interned
		private String countryCode;
		
		/**
		 * Subscription tier - e.g., "FREE", "BASIC", "PREMIUM", "ENTERPRISE".
		 * Low cardinality makes this ideal for @Interned.
		 */
		@Interned
		private String subscriptionTier;
		
		// Constructors
		
		public User() {
		}
		
		public User(String name, String email, String accountStatus, String role, 
		           String countryCode, String subscriptionTier) {
			this.name = name;
			this.email = email;
			this.accountStatus = accountStatus;
			this.role = role;
			this.countryCode = countryCode;
			this.subscriptionTier = subscriptionTier;
		}
		
		// Getters and setters
		
		public Long getId() {
			return id;
		}
		
		public void setId(Long id) {
			this.id = id;
		}
		
		public String getName() {
			return name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getEmail() {
			return email;
		}
		
		public void setEmail(String email) {
			this.email = email;
		}
		
		public String getAccountStatus() {
			return accountStatus;
		}
		
		public void setAccountStatus(String accountStatus) {
			this.accountStatus = accountStatus;
		}
		
		public String getRole() {
			return role;
		}
		
		public void setRole(String role) {
			this.role = role;
		}
		
		public String getCountryCode() {
			return countryCode;
		}
		
		public void setCountryCode(String countryCode) {
			this.countryCode = countryCode;
		}
		
		public String getSubscriptionTier() {
			return subscriptionTier;
		}
		
		public void setSubscriptionTier(String subscriptionTier) {
			this.subscriptionTier = subscriptionTier;
		}
	}
	
	/**
	 * Example entity representing a product with categorization.
	 */
	@Node("Product")
	public static class Product {
		
		@Id
		@GeneratedValue
		private Long id;
		
		private String name;
		private String description;
		private double price;
		
		/**
		 * Product category - e.g., "ELECTRONICS", "CLOTHING", "FOOD", "BOOKS".
		 * Limited number of categories makes this perfect for @Interned.
		 */
		@Interned
		private String category;
		
		/**
		 * Availability status - e.g., "IN_STOCK", "OUT_OF_STOCK", "BACKORDERED".
		 * Low cardinality property ideal for intern().
		 */
		@Interned
		private String availabilityStatus;
		
		/**
		 * Brand - While there are many brands, certain brands are very common.
		 * If your catalog has many products from the same brands, @Interned helps.
		 */
		@Interned
		private String brand;
		
		// Constructors, getters, and setters omitted for brevity
	}
	
	/**
	 * Example showing when NOT to use @Interned.
	 * Do not use @Interned for:
	 * - Unique values (IDs, UUIDs, email addresses)
	 * - High-cardinality strings (full text descriptions, comments)
	 * - Dynamically generated values
	 */
	@Node("BlogPost")
	public static class BlogPost {
		
		@Id
		@GeneratedValue
		private Long id;
		
		// DON'T use @Interned - title is likely unique per post
		private String title;
		
		// DON'T use @Interned - content is unique and long
		private String content;
		
		// DON'T use @Interned - author name might have many variations
		private String authorName;
		
		// DO use @Interned - limited set of categories
		@Interned
		private String category;
		
		// DO use @Interned - limited set of statuses
		@Interned
		private String publishStatus;  // "DRAFT", "PUBLISHED", "ARCHIVED"
	}
}
