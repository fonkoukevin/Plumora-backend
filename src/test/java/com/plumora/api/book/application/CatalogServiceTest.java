package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.infrastructure.BookRepository;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.shared.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTest {

	@Mock
	private BookRepository bookRepository;

	@Mock
	private ChapterRepository chapterRepository;

	private CatalogService catalogService;

	@BeforeEach
	void setUp() {
		catalogService = new CatalogService(bookRepository, chapterRepository);
	}

	@Test
	void getBooksUsesPublishedPublicFilterAndSafePagination() {
		when(bookRepository.findCatalogBooks(eq(BookStatus.PUBLISHED), eq(BookVisibility.PUBLIC), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		catalogService.getBooks(-5, 250);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(bookRepository).findCatalogBooks(eq(BookStatus.PUBLISHED), eq(BookVisibility.PUBLIC), pageableCaptor.capture());

		Pageable pageable = pageableCaptor.getValue();
		assertThat(pageable.getPageNumber()).isZero();
		assertThat(pageable.getPageSize()).isEqualTo(100);
		assertThat(pageable.getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
	}

	@Test
	void searchBooksNormalizesQueryPatternAndGenre() {
		when(bookRepository.searchCatalogBooks(
			eq(BookStatus.PUBLISHED),
			eq(BookVisibility.PUBLIC),
			eq("%plumora%"),
			eq("fantasy"),
			any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of()));

		catalogService.searchBooks("  plumora  ", "  Fantasy  ", 0, 20);

		verify(bookRepository).searchCatalogBooks(
			eq(BookStatus.PUBLISHED),
			eq(BookVisibility.PUBLIC),
			eq("%plumora%"),
			eq("fantasy"),
			any(Pageable.class)
		);
	}

	@Test
	void popularBooksAreSortedByReadingCountThenPublishedAtDescending() {
		when(bookRepository.findCatalogBooks(eq(BookStatus.PUBLISHED), eq(BookVisibility.PUBLIC), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		catalogService.getPopularBooks(0, 20);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(bookRepository).findCatalogBooks(eq(BookStatus.PUBLISHED), eq(BookVisibility.PUBLIC), pageableCaptor.capture());

		Sort sort = pageableCaptor.getValue().getSort();
		assertThat(sort.getOrderFor("readingCount").getDirection()).isEqualTo(Sort.Direction.DESC);
		assertThat(sort.getOrderFor("publishedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void latestBooksAreSortedByPublishedAtDescending() {
		when(bookRepository.findCatalogBooks(eq(BookStatus.PUBLISHED), eq(BookVisibility.PUBLIC), any(Pageable.class)))
			.thenReturn(new PageImpl<>(List.of()));

		catalogService.getLatestBooks(0, 20);

		ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
		verify(bookRepository).findCatalogBooks(eq(BookStatus.PUBLISHED), eq(BookVisibility.PUBLIC), pageableCaptor.capture());

		assertThat(pageableCaptor.getValue().getSort().getOrderFor("publishedAt").getDirection()).isEqualTo(Sort.Direction.DESC);
	}

	@Test
	void getBookReturnsNotFoundWhenBookIsNotInPublicCatalog() {
		UUID bookId = UUID.randomUUID();
		when(bookRepository.findCatalogBookById(bookId, BookStatus.PUBLISHED, BookVisibility.PUBLIC)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> catalogService.getBook(bookId))
			.isInstanceOf(ResourceNotFoundException.class)
			.hasMessage("Catalog book was not found");
	}

	@Test
	void countChaptersDelegatesToChapterRepository() {
		Book book = new Book();
		when(chapterRepository.countByBook(book)).thenReturn(3L);

		assertThat(catalogService.countChapters(book)).isEqualTo(3L);
	}
}
