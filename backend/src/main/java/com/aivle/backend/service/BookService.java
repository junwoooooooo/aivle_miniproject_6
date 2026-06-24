package com.aivle.backend.service;

import com.aivle.backend.entity.Book;
import com.aivle.backend.exception.BookNotFoundException;
import com.aivle.backend.repository.BookRepository;
import com.aivle.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    public List<Book> getBooks() {
        return bookRepository.findAll();
    }

    public Book getBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book createBook(Book book, String username) {
        book.setCreatedBy(username);
        return bookRepository.save(book);
    }

    @Transactional
    public Book updateBook(Long id, Book newData, String username) {
        Book book = getBook(id);
        checkOwner(book, username);
        if (newData.getTitle() != null) book.setTitle(newData.getTitle());
        if (newData.getAuthor() != null) book.setAuthor(newData.getAuthor());
        if (newData.getContent() != null) book.setContent(newData.getContent());
        if (newData.getCoverImageUrl() != null) book.setCoverImageUrl(newData.getCoverImageUrl());
        if (newData.getTags() != null) book.setTags(newData.getTags());
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }

    @Transactional
    public void deleteBook(Long id, String username) {
        Book book = getBook(id);
        checkOwner(book, username);

        reviewRepository.deleteByBookId(id);

        bookRepository.deleteById(id);
    }

    @Transactional
    public Book updateCover(Long id, String url, String username) {
        Book book = getBook(id);
        checkOwner(book, username);
        book.setCoverImageUrl(url);
        book.setUpdatedAt(LocalDateTime.now());
        return bookRepository.save(book);
    }

    @Transactional
    public Book likeBook(Long id) {
        Book book = getBook(id);
        int currentLikes = book.getLikes() == null ? 0 : Math.max(0, book.getLikes());
        book.setLikes(currentLikes + 1);
        return bookRepository.save(book);
    }

    @Transactional
    public Book addViews(Long id) {
        Book book = getBook(id);
        Integer currentViews = book.getViews();
        book.setViews(currentViews == null ? 1 : currentViews + 1);
        return bookRepository.save(book);
    }

    public List<Book> getMyBooks(String username) {
        return bookRepository.findByCreatedBy(username);
    }

    private void checkOwner(Book book, String username) {
        if (book.getCreatedBy() == null || !book.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }
}