package com.plumora.api.reading.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.reading.application.ReadSession;
import com.plumora.api.reading.application.ReadingService;
import com.plumora.api.reading.domain.ReadingProgress;
import com.plumora.api.user.domain.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ReadingControllerTest {

	@Test
	void getReadBookReturnsImportedGutendexChapterForInternalReader() throws Exception {
		ReadingService service = org.mockito.Mockito.mock(ReadingService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ReadingController(service)).build();
		User admin = user("admin@example.com");
		Book book = publishedBook(admin);
		book.setExternalSource(ExternalBookSource.GUTENDEX);
		book.setExternalId("123");
		book.setExternalAuthors(List.of("Victor Hugo"));
		Chapter chapter = chapter(book);
		ReadingProgress progress = progress(user("reader@example.com"), book, chapter);

		when(service.readBook("reader@example.com", book.getId()))
			.thenReturn(new ReadSession(book, List.of(chapter), progress));

		mockMvc.perform(get("/api/v1/books/{bookId}/read", book.getId())
				.contextPath("/api/v1")
				.principal(() -> "reader@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(book.getId().toString()))
			.andExpect(jsonPath("$.externalSource").value("GUTENDEX"))
			.andExpect(jsonPath("$.externalId").value("123"))
			.andExpect(jsonPath("$.externalAuthors[0]").value("Victor Hugo"))
			.andExpect(jsonPath("$.chapters[0].title").value("Texte intégral"))
			.andExpect(jsonPath("$.chapters[0].content").value("Full imported text"))
			.andExpect(jsonPath("$.chapters[0].chapterOrder").value(1));
	}

	private Book publishedBook(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Les Miserables");
		book.setGenre("French fiction");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		return book;
	}

	private Chapter chapter(Book book) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Texte intégral");
		chapter.setContent("Full imported text");
		chapter.setChapterOrder(1);
		chapter.updateWordCount();
		return chapter;
	}

	private ReadingProgress progress(User reader, Book book, Chapter chapter) {
		ReadingProgress progress = new ReadingProgress();
		progress.setId(UUID.randomUUID());
		progress.setUser(reader);
		progress.setBook(book);
		progress.setCurrentChapter(chapter);
		progress.setProgressPercentage(BigDecimal.ZERO);
		progress.setStartedAt(LocalDateTime.now());
		progress.setLastReadAt(LocalDateTime.now());
		return progress;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
