package com.examplatform.subject.repository;

import com.examplatform.subject.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    List<Subject> findAllByAdminIdOrderByCreatedAtDesc(Long adminId);
    Optional<Subject> findByIdAndAdminId(Long id, Long adminId);
    boolean existsByAdminIdAndNameIgnoreCase(Long adminId, String name);
}