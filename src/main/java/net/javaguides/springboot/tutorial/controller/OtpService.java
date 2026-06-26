package net.javaguides.springboot.tutorial.controller;

import net.javaguides.springboot.tutorial.entity.OtpToken;
import net.javaguides.springboot.tutorial.repository.OtpTokenRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class OtpService {
    
    private final OtpTokenRepository repo;

    public OtpService(OtpTokenRepository repo) { 
        this.repo = repo; 
    }

    public String generateAndStoreOtp(String email) {
        repo.deleteByEmail(email); // Clean previous tokens
        String otp = String.format("%06d", new Random().nextInt(999999));
        
        OtpToken token = new OtpToken();
        token.setEmail(email);
        token.setOtpCode(otp);
        token.setExpirationTime(LocalDateTime.now().plusMinutes(10));
        repo.save(token);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        Optional<OtpToken> tokenOpt = repo.findByEmailAndOtpCode(email, otp);
        if (tokenOpt.isPresent()) {
            OtpToken token = tokenOpt.get();
            if (token.getExpirationTime().isAfter(LocalDateTime.now())) {
                repo.delete(token); // Burn after reading
                return true;
            }
            repo.delete(token); // Delete expired token
        }
        return false;
    }
}