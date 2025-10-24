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

package org.springframework.data.falkordb.integration;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.falkordb.core.schema.GeneratedValue;
import org.springframework.data.falkordb.core.schema.Id;
import org.springframework.data.falkordb.core.schema.InternalIdGenerator;
import org.springframework.data.falkordb.core.schema.Node;
import org.springframework.data.falkordb.core.schema.Property;
import org.springframework.data.falkordb.core.schema.Relationship;

/**
 * Twitter User entity for FalkorDB integration testing.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node(labels = { "User", "TwitterUser" })
public class TwitterUser {

	@Id
	@GeneratedValue(generatorClass = InternalIdGenerator.class)
	private Long id;

	@Property("username")
	private String username;

	@Property("display_name")
	private String displayName;

	@Property("email")
	private String email;

	@Property("bio")
	private String bio;

	@Property("follower_count")
	private Integer followerCount;

	@Property("following_count")
	private Integer followingCount;

	@Property("tweet_count")
	private Integer tweetCount;

	@Property("verified")
	private Boolean verified;

	@Property("created_at")
	private LocalDateTime createdAt;

	@Property("location")
	private String location;

	// Relationships
	@Relationship(value = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
	private List<TwitterUser> following;

	@Relationship(value = "FOLLOWS", direction = Relationship.Direction.INCOMING)
	private List<TwitterUser> followers;

	@Relationship(value = "POSTED", direction = Relationship.Direction.OUTGOING)
	private List<Tweet> tweets;

	@Relationship(value = "LIKED", direction = Relationship.Direction.OUTGOING)
	private List<Tweet> likedTweets;

	@Relationship(value = "RETWEETED", direction = Relationship.Direction.OUTGOING)
	private List<Tweet> retweetedTweets;

	// Constructors
	public TwitterUser() {
	}

	public TwitterUser(String username, String displayName, String email) {
		this.username = username;
		this.displayName = displayName;
		this.email = email;
		this.createdAt = LocalDateTime.now();
		this.followerCount = 0;
		this.followingCount = 0;
		this.tweetCount = 0;
		this.verified = false;
	}

	// Getters and Setters
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return this.username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getDisplayName() {
		return this.displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getEmail() {
		return this.email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getBio() {
		return this.bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}

	public Integer getFollowerCount() {
		return this.followerCount;
	}

	public void setFollowerCount(Integer followerCount) {
		this.followerCount = followerCount;
	}

	public Integer getFollowingCount() {
		return this.followingCount;
	}

	public void setFollowingCount(Integer followingCount) {
		this.followingCount = followingCount;
	}

	public Integer getTweetCount() {
		return this.tweetCount;
	}

	public void setTweetCount(Integer tweetCount) {
		this.tweetCount = tweetCount;
	}

	public Boolean getVerified() {
		return this.verified;
	}

	public void setVerified(Boolean verified) {
		this.verified = verified;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public String getLocation() {
		return this.location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public List<TwitterUser> getFollowing() {
		return this.following;
	}

	public void setFollowing(List<TwitterUser> following) {
		this.following = following;
	}

	public List<TwitterUser> getFollowers() {
		return this.followers;
	}

	public void setFollowers(List<TwitterUser> followers) {
		this.followers = followers;
	}

	public List<Tweet> getTweets() {
		return this.tweets;
	}

	public void setTweets(List<Tweet> tweets) {
		this.tweets = tweets;
	}

	public List<Tweet> getLikedTweets() {
		return this.likedTweets;
	}

	public void setLikedTweets(List<Tweet> likedTweets) {
		this.likedTweets = likedTweets;
	}

	public List<Tweet> getRetweetedTweets() {
		return this.retweetedTweets;
	}

	public void setRetweetedTweets(List<Tweet> retweetedTweets) {
		this.retweetedTweets = retweetedTweets;
	}

	@Override
	public String toString() {
		return "TwitterUser{" + "id=" + this.id + ", username='" + this.username + '\'' + ", displayName='"
				+ this.displayName + '\'' + ", followerCount=" + this.followerCount + ", followingCount="
				+ this.followingCount + ", verified=" + this.verified + '}';
	}

}
