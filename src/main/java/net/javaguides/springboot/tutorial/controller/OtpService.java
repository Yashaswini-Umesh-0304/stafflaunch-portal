package net.javaguides.springboot.tutorial.controller;

import org.springframework.stereotype.Service;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class OtpService {
    
    // Temporarily stores OTPs in memory mapped to the user's email
    private final ConcurrentHashMap<String, String> otpStorage = new ConcurrentHashMap<>();

    public String generateAndStoreOtp(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        otpStorage.put(email, otp);
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        if (otpStorage.containsKey(email) && otpStorage.get(email).equals(otp)) {
            otpStorage.remove(email); // Consume the OTP so it can't be reused
            return true;
        }
        return false;
    }
}