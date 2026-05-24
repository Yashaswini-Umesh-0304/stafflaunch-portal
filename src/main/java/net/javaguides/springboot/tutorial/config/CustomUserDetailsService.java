package net.javaguides.springboot.tutorial.security; 

import net.javaguides.springboot.tutorial.entity.Employee;
import net.javaguides.springboot.tutorial.repository.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository repository;

    public CustomUserDetailsService(EmployeeRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // 1. Search by BOTH email and username
        Employee employee = repository.findByEmailOrUsername(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // 2. Safely handle passwords (so Spring doesn't crash on pre-approved accounts with no password yet)
        String password = employee.getPassword() != null ? employee.getPassword() : "";

        // 3. Map the entity to Spring Security's strict User object
        return new User(
                employee.getEmail(),
                password,
                employee.isEnabled(), // CRITICAL: Tells Spring if the Admin has approved them
                true,
                true,
                true,
                Collections.singleton(new SimpleGrantedAuthority(employee.getRole()))
        );
    }
}