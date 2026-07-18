package com.plumora.api.book.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.plumora.api.shared.exception.BusinessException;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class LocalBookCoverStorageTest {

	@TempDir
	private Path uploadDir;

	@Test
	void storesImageAndReturnsRelativeCoverUrl() throws Exception {
		LocalBookCoverStorage storage = new LocalBookCoverStorage(
			uploadDir.toString(),
			"uploads/book-covers",
			5_242_880
		);
		MockMultipartFile image = new MockMultipartFile(
			"coverImage",
			"cover.png",
			MediaType.IMAGE_PNG_VALUE,
			new byte[] {1, 2, 3}
		);

		String coverUrl = storage.store(image);

		assertThat(coverUrl).startsWith("uploads/book-covers/");
		String filename = coverUrl.substring("uploads/book-covers/".length());
		assertThat(Files.exists(uploadDir.resolve("book-covers").resolve(filename))).isTrue();
		assertThat(storage.load(filename).exists()).isTrue();
		assertThat(storage.mediaType(filename)).isEqualTo(MediaType.IMAGE_PNG);
	}

	@Test
	void rejectsNonImageFiles() {
		LocalBookCoverStorage storage = new LocalBookCoverStorage(
			uploadDir.toString(),
			"uploads/book-covers",
			5_242_880
		);
		MockMultipartFile textFile = new MockMultipartFile(
			"coverImage",
			"cover.txt",
			MediaType.TEXT_PLAIN_VALUE,
			new byte[] {1, 2, 3}
		);

		assertThatThrownBy(() -> storage.store(textFile))
			.isInstanceOf(BusinessException.class)
			.hasMessage("Book cover must be a JPEG, PNG, WebP or GIF image");
	}

	@Test
	void loadsBundledDemoCoverWhenNoUploadedFileExists() {
		LocalBookCoverStorage storage = new LocalBookCoverStorage(
			uploadDir.toString(),
			"uploads/book-covers",
			5_242_880
		);

		assertThat(storage.load("plumora-lumen.png").exists()).isTrue();
		assertThat(storage.mediaType("plumora-lumen.png")).isEqualTo(MediaType.IMAGE_PNG);
	}

	@Test
	void loadsExpandedCatalogCoversWithExpectedDimensions() throws Exception {
		LocalBookCoverStorage storage = new LocalBookCoverStorage(
			uploadDir.toString(),
			"uploads/book-covers",
			5_242_880
		);
		List<String> coverFilenames = List.of(
			"plumora-heures-ambre.png",
			"plumora-maison-marees.png",
			"plumora-orbite-neuf.png",
			"plumora-memoire-titan.png",
			"plumora-cafe-jours-pluvieux.png",
			"plumora-dansent-sous-pluie.png",
			"plumora-disparus-canal.png",
			"plumora-chambre-314.png",
			"plumora-royaume-cerfs-volants.png",
			"plumora-bibliotheque-nuages.png"
		);

		for (String coverFilename : coverFilenames) {
			assertThat(storage.load(coverFilename).exists()).as(coverFilename).isTrue();
			assertThat(storage.mediaType(coverFilename)).as(coverFilename).isEqualTo(MediaType.IMAGE_PNG);
			BufferedImage image = ImageIO.read(storage.load(coverFilename).getInputStream());
			assertThat(image).as(coverFilename).isNotNull();
			assertThat(image.getWidth()).as(coverFilename + " width").isEqualTo(600);
			assertThat(image.getHeight()).as(coverFilename + " height").isEqualTo(900);
		}
	}
}
