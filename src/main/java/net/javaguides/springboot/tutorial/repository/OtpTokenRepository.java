package net.javaguides.springboot.tutorial.repository;

import net.javaguides.springboot.tutorial.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {
    Optional<OtpToken> findByEmailAndOtpCode(String email, String otpCode);
    
    @Transactional
    void deleteByEmail(String email);
}