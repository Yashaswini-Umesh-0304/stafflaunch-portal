package net.javaguides.springboot.tutorial.repository;

import net.javaguides.springboot.tutorial.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {
    
    Optional<Student> findByEmail(String email);

    // This allows the system to find the user whether they type their email OR their username
    @Query("SELECT s FROM Student s WHERE s.email = ?1 OR s.username = ?1")
    Optional<Student> findByEmailOrUsername(String usernameOrEmail);
}