package com.aivle.backend.service;

import com.aivle.backend.entity.Review;
import com.aivle.backend.exception.ReviewNotFoundException;
import com.aivle.backend.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public List<Review> getReviews(Long bookId) {
        if (bookId != null) {
            return reviewRepository.findByBookId(bookId);
        }
        return reviewRepository.findAll();
    }

    public Review createReview(Review review, String username) {
        if (review.getLikes() == null) review.setLikes(0);
        LocalDateTime now = LocalDateTime.now();
        if (review.getCreatedAt() == null) review.setCreatedAt(now);
        review.setUpdatedAt(now);
        if (username != null) review.setCreatedBy(username);
        return reviewRepository.save(review);
    }

    @Transactional
    public Review updateReview(Long id, Review request, String username) {
        Review review = getReview(id);
        checkOwner(review, username);
        if (request.getContent() != null) review.setContent(request.getContent());
        if (request.getNickname() != null) review.setNickname(request.getNickname());
        review.setUpdatedAt(LocalDateTime.now());
        return reviewRepository.save(review);
    }

    @Transactional
    public void deleteReview(Long id, String username) {
        Review review = getReview(id);
        checkOwner(review, username);
        reviewRepository.delete(review);
    }
    
    public List<Review> getMyReviews(String username) {
        return reviewRepository.findByCreatedBy(username);
    }

    private Review getReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ReviewNotFoundException(id));
    }

    private void checkOwner(Review review, String username) {
        if (review.getCreatedBy() == null || !review.getCreatedBy().equals(username)) {
            throw new AccessDeniedException("권한이 없습니다.");
        }
    }

    @Transactional
    public Review likeReview(Long id, Integer likes) {
        Review review = getReview(id);
        if (likes != null) {
            review.setLikes(Math.max(0, likes));
        } else {
            Integer currentLikes = review.getLikes();
            review.setLikes(currentLikes == null ? 1 : currentLikes + 1);
        }
        return reviewRepository.save(review);
    }
}