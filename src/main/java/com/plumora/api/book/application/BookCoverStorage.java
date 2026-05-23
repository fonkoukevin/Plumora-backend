package com.plumora.api.book.application;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface BookCoverStorage {
	String store(MultipartFile file);

	Resource load(String filename);

	MediaType mediaType(String filename);
}
