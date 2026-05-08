package net.javaguides.springboot.tutorial.security; 

import net.javaguides.springboot.tutorial.entity.Student;
import net.javaguides.springboot.tutorial.repository.StudentRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final StudentRepository repository;

    public CustomUserDetailsService(StudentRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 1. Search by BOTH email and username
        Student student = repository.findByEmailOrUsername(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2. Safely handle passwords (so Spring doesn't crash on pre-approved accounts with no password yet)
        String password = student.getPassword() != null ? student.getPassword() : "";

        // 3. Map the entity to Spring Security's strict User object
        return new User(
                student.getEmail(),
                password,
                student.isEnabled(), // CRITICAL: Tells Spring if the Admin has approved them
                true,
                true,
                true,
                Collections.singleton(new SimpleGrantedAuthority(student.getRole()))
        );
    }
}