package net.javaguides.springboot.tutorial.repository;

import net.javaguides.springboot.tutorial.entity.TechIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TechIssueRepository extends JpaRepository<TechIssue, Long> {
    List<TechIssue> findByEmployeeIdOrderByReportDateDesc(Long employeeId);
    List<TechIssue> findAllByOrderByReportDateDesc();
}