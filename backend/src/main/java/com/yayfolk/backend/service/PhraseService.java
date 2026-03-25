package com.yayfolk.backend.service;

import com.yayfolk.backend.entity.Phrase;
import com.yayfolk.backend.entity.User;
import com.yayfolk.backend.repository.PhraseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PhraseService {
    @Autowired
    private PhraseRepository phraseRepository;
    
    public Phrase createPhrase(User user, String text, String originalText, String category) {
        Phrase phrase = new Phrase();
        phrase.setUser(user);
        phrase.setText(text);
        phrase.setOriginalText(originalText);
        phrase.setCategory(category);
        return phraseRepository.save(phrase);
    }
    
    public List<Phrase> getPhrasesByUser(User user) {
        return phraseRepository.findByUser(user);
    }
    
    public List<Phrase> getPhrasesByUserAndCategory(User user, String category) {
        return phraseRepository.findByUserAndCategory(user, category);
    }
    
    public void deletePhrase(Long phraseId, User user) {
        Phrase phrase = phraseRepository.findById(phraseId).orElseThrow(() -> new RuntimeException("Phrase not found"));
        if (!phrase.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You can only delete your own phrases");
        }
        phraseRepository.delete(phrase);
    }
}
