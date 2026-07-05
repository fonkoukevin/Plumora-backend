package com.plumora.api.book.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.plumora.api.book.application.ExternalBook;
import com.plumora.api.book.application.ExternalBookFilter;
import com.plumora.api.book.application.ExternalBookService;
import com.plumora.api.book.application.ImportedExternalBookResult;
import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.ExternalBookSource;
import com.plumora.api.user.domain.User;
import java.lang.reflect.Method;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ExternalBookControllerTest {

	@Test
	void getExternalBooksReturnsPageOfMappedDtos() throws Exception {
		ExternalBookService service = org.mockito.Mockito.mock(ExternalBookService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExternalBookController(service)).build();
		UUID internalBookId = UUID.randomUUID();
		ExternalBook book = new ExternalBook(
			"123",
			"GUTENDEX",
			"Les Miserables",
			List.of("Victor Hugo"),
			"Un roman social.",
			List.of("French fiction"),
			List.of("fr"),
			false,
			"Text",
			42,
			"https://covers.openlibrary.org/b/id/987-L.jpg?default=false",
			"https://gutendex.test/book.html",
			Map.of("text/html", "https://gutendex.test/book.html"),
			"https://www.gutenberg.org/ebooks/123",
			true,
			internalBookId
		);
		when(service.searchExternalBooks("Hugo", "fr", "fiction", 0))
			.thenReturn(new PageImpl<>(List.of(book), PageRequest.of(0, 32), 1));

		mockMvc.perform(get("/api/v1/external-books")
				.contextPath("/api/v1")
				.param("search", "Hugo")
				.param("language", "fr")
				.param("topic", "fiction")
				.param("page", "0"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].externalId").value("123"))
			.andExpect(jsonPath("$.content[0].source").value("GUTENDEX"))
			.andExpect(jsonPath("$.content[0].authors[0]").value("Victor Hugo"))
			.andExpect(jsonPath("$.content[0].coverUrl").value("https://covers.openlibrary.org/b/id/987-L.jpg?default=false"))
			.andExpect(jsonPath("$.content[0].readUrl").value("https://gutendex.test/book.html"))
			.andExpect(jsonPath("$.content[0].imported").value(true))
			.andExpect(jsonPath("$.content[0].internalBookId").value(internalBookId.toString()))
			.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void getExternalBooksAcceptsGenreAliasForDiscoverFilters() throws Exception {
		ExternalBookService service = org.mockito.Mockito.mock(ExternalBookService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExternalBookController(service)).build();
		when(service.searchExternalBooks(null, null, "fantasy", 0))
			.thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 32), 0));

		mockMvc.perform(get("/api/v1/external-books")
				.contextPath("/api/v1")
				.param("genre", "fantasy"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content").isArray());

		verify(service).searchExternalBooks(null, null, "fantasy", 0);
	}

	@Test
	void getExternalBookFiltersReturnsDiscoverChips() throws Exception {
		ExternalBookService service = org.mockito.Mockito.mock(ExternalBookService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExternalBookController(service)).build();
		when(service.getDiscoverFilters()).thenReturn(List.of(
			new ExternalBookFilter("Tous", null),
			new ExternalBookFilter("Fantasy", "fantasy"),
			new ExternalBookFilter("Sci-Fi", "science fiction")
		));

		mockMvc.perform(get("/api/v1/external-books/filters")
				.contextPath("/api/v1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].label").value("Tous"))
			.andExpect(jsonPath("$[0].topic").value(nullValue()))
			.andExpect(jsonPath("$[1].label").value("Fantasy"))
			.andExpect(jsonPath("$[1].topic").value("fantasy"))
			.andExpect(jsonPath("$[2].label").value("Sci-Fi"))
			.andExpect(jsonPath("$[2].topic").value("science fiction"));
	}

	@Test
	void importGutendexBookRequiresOnlyAuthenticatedUser() throws Exception {
		Method method = ExternalBookController.class.getMethod("importGutendexBook", Principal.class, int.class);

		PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

		assertThat(preAuthorize).isNotNull();
		assertThat(preAuthorize.value()).isEqualTo("isAuthenticated()");
	}

	@Test
	void importGutendexBookCanBeRequestedByReaderAndReturnsExistingBook() throws Exception {
		ExternalBookService service = org.mockito.Mockito.mock(ExternalBookService.class);
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ExternalBookController(service)).build();
		Book book = importedBook();
		when(service.importGutendexBook("reader@example.com", 123))
			.thenReturn(new ImportedExternalBookResult(book, false));

		mockMvc.perform(post("/api/v1/books/import/gutendex/123")
				.contextPath("/api/v1")
				.principal(() -> "reader@example.com"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(book.getId().toString()))
			.andExpect(jsonPath("$.externalSource").value("GUTENDEX"))
			.andExpect(jsonPath("$.externalId").value("123"));

		verify(service).importGutendexBook("reader@example.com", 123);
	}

	private Book importedBook() {
		User reader = new User();
		reader.setId(UUID.randomUUID());
		reader.setUsername("reader");
		reader.setEmail("reader@example.com");
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(reader);
		book.setTitle("Les Miserables");
		book.setGenre("French fiction");
		book.setStatus(BookStatus.PUBLISHED);
		book.setVisibility(BookVisibility.PUBLIC);
		book.setPublishedAt(LocalDateTime.now());
		book.setExternalSource(ExternalBookSource.GUTENDEX);
		book.setExternalId("123");
		return book;
	}
}
