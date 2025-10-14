/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Tweet entity for FalkorDB integration testing.
 *
 * @author Shahar Biron (FalkorDB adaptation)
 * @since 1.0
 */
@Node(labels = { "Tweet" })
public class Tweet {

	@Id
	@GeneratedValue(generatorClass = InternalIdGenerator.class)
	private Long id;

	@Property("text")
	private String text;

	@Property("created_at")
	private LocalDateTime createdAt;

	@Property("like_count")
	private Integer likeCount;

	@Property("retweet_count")
	private Integer retweetCount;

	@Property("reply_count")
	private Integer replyCount;

	@Property("is_retweet")
	private Boolean isRetweet;

	@Property("is_reply")
	private Boolean isReply;

	// Relationships
	@Relationship(value = "POSTED", direction = Relationship.Direction.INCOMING)
	private TwitterUser author;

	@Relationship(value = "LIKED", direction = Relationship.Direction.INCOMING)
	private List<TwitterUser> likedBy;

	@Relationship(value = "RETWEETED", direction = Relationship.Direction.INCOMING)
	private List<TwitterUser> retweetedBy;

	@Relationship(value = "MENTIONS", direction = Relationship.Direction.OUTGOING)
	private List<TwitterUser> mentions;

	@Relationship(value = "REPLIES_TO", direction = Relationship.Direction.OUTGOING)
	private Tweet replyToTweet;

	@Relationship(value = "REPLIES_TO", direction = Relationship.Direction.INCOMING)
	private List<Tweet> replies;

	@Relationship(value = "RETWEET_OF", direction = Relationship.Direction.OUTGOING)
	private Tweet originalTweet;

	@Relationship(value = "HAS_HASHTAG", direction = Relationship.Direction.OUTGOING)
	private List<Hashtag> hashtags;

	// Constructors
	public Tweet() {
	}

	public Tweet(String text, TwitterUser author) {
		this.text = text;
		this.author = author;
		this.createdAt = LocalDateTime.now();
		this.likeCount = 0;
		this.retweetCount = 0;
		this.replyCount = 0;
		this.isRetweet = false;
		this.isReply = false;
	}

	// Getters and Setters
	public Long getId() {
		return this.id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getText() {
		return this.text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public LocalDateTime getCreatedAt() {
		return this.createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public Integer getLikeCount() {
		return this.likeCount;
	}

	public void setLikeCount(Integer likeCount) {
		this.likeCount = likeCount;
	}

	public Integer getRetweetCount() {
		return this.retweetCount;
	}

	public void setRetweetCount(Integer retweetCount) {
		this.retweetCount = retweetCount;
	}

	public Integer getReplyCount() {
		return this.replyCount;
	}

	public void setReplyCount(Integer replyCount) {
		this.replyCount = replyCount;
	}

	public Boolean getIsRetweet() {
		return this.isRetweet;
	}

	public void setIsRetweet(Boolean isRetweet) {
		this.isRetweet = isRetweet;
	}

	public Boolean getIsReply() {
		return this.isReply;
	}

	public void setIsReply(Boolean isReply) {
		this.isReply = isReply;
	}

	public TwitterUser getAuthor() {
		return this.author;
	}

	public void setAuthor(TwitterUser author) {
		this.author = author;
	}

	public List<TwitterUser> getLikedBy() {
		return this.likedBy;
	}

	public void setLikedBy(List<TwitterUser> likedBy) {
		this.likedBy = likedBy;
	}

	public List<TwitterUser> getRetweetedBy() {
		return this.retweetedBy;
	}

	public void setRetweetedBy(List<TwitterUser> retweetedBy) {
		this.retweetedBy = retweetedBy;
	}

	public List<TwitterUser> getMentions() {
		return this.mentions;
	}

	public void setMentions(List<TwitterUser> mentions) {
		this.mentions = mentions;
	}

	public Tweet getReplyToTweet() {
		return this.replyToTweet;
	}

	public void setReplyToTweet(Tweet replyToTweet) {
		this.replyToTweet = replyToTweet;
	}

	public List<Tweet> getReplies() {
		return this.replies;
	}

	public void setReplies(List<Tweet> replies) {
		this.replies = replies;
	}

	public Tweet getOriginalTweet() {
		return this.originalTweet;
	}

	public void setOriginalTweet(Tweet originalTweet) {
		this.originalTweet = originalTweet;
	}

	public List<Hashtag> getHashtags() {
		return this.hashtags;
	}

	public void setHashtags(List<Hashtag> hashtags) {
		this.hashtags = hashtags;
	}

	@Override
	public String toString() {
		return "Tweet{" + "id=" + this.id + ", text='" + this.text + '\'' + ", createdAt=" + this.createdAt
				+ ", likeCount=" + this.likeCount + ", retweetCount=" + this.retweetCount + ", author="
				+ ((this.author != null) ? this.author.getUsername() : "null") + '}';
	}

}
