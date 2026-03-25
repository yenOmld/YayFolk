package com.yayfolk.backend.repository;

import com.yayfolk.backend.entity.Phrase;
import com.yayfolk.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PhraseRepository extends JpaRepository<Phrase, Long> {
    List<Phrase> findByUser(User user);
    List<Phrase> findByUserAndCategory(User user, String category);
}
