package com.plumora.api.ai.presentation;

import static org.assertj.core.api.Assertions.assertThat;

import com.plumora.api.ai.domain.AiWritingRequest;
import com.plumora.api.ai.domain.AiWritingSuggestion;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.Chapter;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AiWritingMapperTest {
    @Test
    void persistedSuggestionCanBeCorrelatedWithOriginalPassageAndChapter() {
        Book book = new Book();
        book.setId(UUID.randomUUID());
        Chapter chapter = new Chapter();
        chapter.setId(UUID.randomUUID());
        chapter.setBook(book);
        AiWritingRequest request = new AiWritingRequest();
        request.setId(UUID.randomUUID());
        request.setChapter(chapter);
        request.setSelectedText("Original passage");
        request.setContextText("Surrounding paragraph");
        AiWritingSuggestion suggestion = new AiWritingSuggestion();
        suggestion.setId(UUID.randomUUID());
        suggestion.setRequest(request);
        suggestion.setSuggestionText("Proposed passage");

        var created = AiWritingMapper.toSuggestionResponse(suggestion);
        var detail = AiWritingMapper.toRequestResponse(request, List.of(suggestion));

        assertThat(created.requestId()).isEqualTo(detail.id());
        assertThat(created.chapterId()).isEqualTo(chapter.getId());
        assertThat(created.bookId()).isEqualTo(book.getId());
        assertThat(detail.selectedText()).isEqualTo("Original passage");
        assertThat(detail.contextText()).isEqualTo("Surrounding paragraph");
        assertThat(detail.suggestions().getFirst().suggestionText()).isEqualTo("Proposed passage");
    }
}
