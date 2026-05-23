package com.plumora.api.book.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.plumora.api.book.domain.Book;
import com.plumora.api.book.domain.BookStatus;
import com.plumora.api.book.domain.BookVisibility;
import com.plumora.api.book.domain.Chapter;
import com.plumora.api.book.infrastructure.ChapterRepository;
import com.plumora.api.book.presentation.UpdateChapterRequest;
import com.plumora.api.shared.exception.UnauthorizedActionException;
import com.plumora.api.user.domain.User;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

	@Mock
	private ChapterRepository chapterRepository;

	@Mock
	private BookService bookService;

	private ChapterService chapterService;

	@BeforeEach
	void setUp() {
		chapterService = new ChapterService(chapterRepository, bookService);
	}

	@Test
	void authorCanUpdateOwnChapter() {
		User author = user("author@example.com");
		Book book = book(author);
		Chapter chapter = chapter(book);

		when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
		when(bookService.getOwnedEditableBook(author.getEmail(), book.getId())).thenReturn(book);
		when(chapterRepository.save(chapter)).thenReturn(chapter);

		Chapter updated = chapterService.updateChapter(
			author.getEmail(),
			chapter.getId(),
			new UpdateChapterRequest("Nouveau titre", "Un texte mis a jour")
		);

		assertThat(updated.getTitle()).isEqualTo("Nouveau titre");
		assertThat(updated.getContent()).isEqualTo("Un texte mis a jour");
		assertThat(updated.getWordCount()).isEqualTo(5);
	}

	@Test
	void anotherAuthorCannotUpdateChapter() {
		User author = user("author@example.com");
		Book book = book(author);
		Chapter chapter = chapter(book);

		when(chapterRepository.findById(chapter.getId())).thenReturn(Optional.of(chapter));
		when(bookService.getOwnedEditableBook("other@example.com", book.getId()))
			.thenThrow(new UnauthorizedActionException("Only the author can manage this book"));

		assertThatThrownBy(() -> chapterService.updateChapter(
			"other@example.com",
			chapter.getId(),
			new UpdateChapterRequest("Nope", "Nope")
		))
			.isInstanceOf(UnauthorizedActionException.class)
			.hasMessage("Only the author can manage this book");
	}

	private Chapter chapter(Book book) {
		Chapter chapter = new Chapter();
		chapter.setId(UUID.randomUUID());
		chapter.setBook(book);
		chapter.setTitle("Chapitre 1");
		chapter.setContent("Ancien texte");
		chapter.setChapterOrder(1);
		chapter.updateWordCount();
		return chapter;
	}

	private Book book(User author) {
		Book book = new Book();
		book.setId(UUID.randomUUID());
		book.setAuthor(author);
		book.setTitle("Livre");
		book.setGenre("Fantasy");
		book.setStatus(BookStatus.DRAFT);
		book.setVisibility(BookVisibility.PRIVATE);
		return book;
	}

	private User user(String email) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setEmail(email);
		user.setUsername(email.substring(0, email.indexOf('@')));
		return user;
	}
}
