package com.cheapradar.backend.repository;

import com.cheapradar.backend.model.Search;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<Search, String> {
    List<Search> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<Search> findAllByNextCheckAtBefore(LocalDateTime time);

}
