package com.aivle.backend.config;

import com.aivle.backend.entity.Book;
import com.aivle.backend.entity.Review;
import com.aivle.backend.repository.BookRepository;
import com.aivle.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {

        if (bookRepository.count() > 0) {
            System.out.println("이미 책 데이터가 존재합니다.");
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();

        InputStream inputStream = new ClassPathResource("db.json").getInputStream();
        JsonNode root = objectMapper.readTree(inputStream);

        Map<String, Long> oldBookIdToNewBookId = new HashMap<>();

        JsonNode books = root.get("books");

        if (books == null || !books.isArray()) {
            System.out.println("db.json에 books 데이터가 없습니다.");
            return;
        }

        for (JsonNode node : books) {
            Book book = new Book();

            String oldBookId = node.get("id").asText();

            book.setTitle(node.get("title").asText());
            book.setAuthor(node.get("author").asText());

            if (node.has("likes")) {
                book.setLikes(node.get("likes").asInt());
            }

            if (node.has("content")) {
                book.setContent(node.get("content").asText());
            }

            if (node.has("coverImageUrl")) {
                book.setCoverImageUrl(node.get("coverImageUrl").asText());
            }

            if (node.has("createdAt")) {
                book.setCreatedAt(parseDateTime(node.get("createdAt").asText()));
            }

            if (node.has("updatedAt")) {
                book.setUpdatedAt(parseDateTime(node.get("updatedAt").asText()));
            }

            if (node.has("tags") && node.get("tags").isArray()) {
                for (JsonNode tagNode : node.get("tags")) {
                    book.getTags().add(tagNode.asText());
                }
            }

            Book savedBook = bookRepository.save(book);

            // 기존 db.json의 book id와 새로 생성된 H2 book id 연결
            oldBookIdToNewBookId.put(oldBookId, savedBook.getId());
        }


        JsonNode reviews = root.get("reviews");

        if (reviews != null && reviews.isArray()) {
            for (JsonNode node : reviews) {
                Review review = new Review();

                String oldBookId = node.get("bookId").asText();
                Long newBookId = oldBookIdToNewBookId.get(oldBookId);

                // 해당 bookId가 books에 없으면 저장하지 않음
                if (newBookId == null) {
                    continue;
                }

                review.setBookId(newBookId);

                if (node.has("nickname")) {
                    review.setNickname(node.get("nickname").asText());
                }

                if (node.has("content")) {
                    review.setContent(node.get("content").asText());
                }

                if (node.has("likes")) {
                    review.setLikes(node.get("likes").asInt());
                }

                if (node.has("createdAt")) {
                    review.setCreatedAt(parseDateTime(node.get("createdAt").asText()));
                }

                if (node.has("updatedAt")) {
                    review.setUpdatedAt(parseDateTime(node.get("updatedAt").asText()));
                }

                reviewRepository.save(review);
            }
        }

        System.out.println("db.json books, reviews 데이터 H2 DB 저장 완료");
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
}