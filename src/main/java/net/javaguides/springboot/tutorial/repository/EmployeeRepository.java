package net.javaguides.springboot.tutorial.repository;

import net.javaguides.springboot.tutorial.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
    Optional<Employee> findByEmail(String email);

    // This allows the system to find the user whether they type their email OR their username
    @Query("SELECT e FROM Employee e WHERE e.email = ?1 OR e.username = ?1")
    Optional<Employee> findByEmailOrUsername(String usernameOrEmail);
}