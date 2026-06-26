package net.javaguides.springboot.tutorial.repository;

import net.javaguides.springboot.tutorial.entity.HardwareBundle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HardwareBundleRepository extends JpaRepository<HardwareBundle, Long> {
    Optional<HardwareBundle> findByDescription(String description);
}