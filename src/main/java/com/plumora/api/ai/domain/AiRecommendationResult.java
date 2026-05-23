package com.plumora.api.ai.domain;

import com.plumora.api.book.domain.Book;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_recommendation_results")
public class AiRecommendationResult {

	@Id
	@GeneratedValue
	@Column(name = "id_ai_recommendation_result")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "request_id", nullable = false)
	private AiRecommendationRequestEntity request;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "book_id", nullable = false)
	private Book book;

	@Column(name = "match_score", nullable = false)
	private int matchScore;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "reasons", columnDefinition = "jsonb")
	private List<String> reasons = new ArrayList<>();

	@Column(name = "rank_position", nullable = false)
	private int rankPosition;
}
