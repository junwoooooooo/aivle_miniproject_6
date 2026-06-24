package com.aivle.backend.controller;

import com.aivle.backend.entity.Book;
import com.aivle.backend.service.BookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping
    public List<Book> getBooks(@RequestParam(required = false) String createdBy) {
        if (createdBy != null) {
            return bookService.getMyBooks(createdBy);
        }
        return bookService.getBooks();
    }

    @GetMapping("/{id}")
    public Book getBook(@PathVariable Long id) {
        return bookService.getBook(id);
    }

    @PostMapping
    public ResponseEntity<Book> createBook(@Valid @RequestBody Book book, Principal principal) {
        return ResponseEntity.status(201).body(bookService.createBook(book, principal.getName()));
    }

    @PatchMapping("/{id}")
    public Book updateBook(@PathVariable Long id, @RequestBody Book book, Principal principal) {
        return bookService.updateBook(id, book, principal.getName());
    }

    @PatchMapping("/{id}/likes")
    public Book likeBook(@PathVariable Long id, @RequestBody(required = false) Book body) {
        return bookService.likeBook(id, body != null ? body.getLikes() : null);
    }

    @PatchMapping("/{id}/cover")
    public Book updateCover(@PathVariable Long id, @RequestBody Book book, Principal principal) {
        return bookService.updateCover(id, book.getCoverImageUrl(), principal.getName());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id, Principal principal) {
        bookService.deleteBook(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/views")
    public ResponseEntity<?> addViews(@PathVariable Long id, HttpServletRequest request) {
        String clientIp = extractClientIp(request);
        Book updatedBook = bookService.addViews(id);
        return ResponseEntity.ok(updatedBook);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");

        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }
}