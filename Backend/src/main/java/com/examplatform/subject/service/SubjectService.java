package com.examplatform.subject.service;

import com.examplatform.subject.entity.Subject;
import com.examplatform.subject.repository.SubjectRepository;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SubjectService {

    private final SubjectRepository subjectRepository;
    private final UserRepository userRepository;

    public SubjectService(
            SubjectRepository subjectRepository,
            UserRepository userRepository) {
        this.subjectRepository = subjectRepository;
        this.userRepository = userRepository;
    }

    public Subject createSubject(Long adminId, String name, String description) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (!"ADMIN".equals(admin.getRole().name())) {
            throw new IllegalArgumentException("Only admins can create subjects");
        }

        if (subjectRepository.existsByAdminIdAndNameIgnoreCase(adminId, name.trim())) {
            throw new IllegalArgumentException("A subject with this name already exists");
        }

        Subject subject = new Subject(name, description, admin);
        return subjectRepository.save(subject);
    }

    @Transactional(readOnly = true)
    public List<Subject> getSubjects(Long adminId) {
        return subjectRepository.findAllByAdminIdOrderByCreatedAtDesc(adminId);
    }

    public Subject updateSubject(
            Long adminId,
            Long subjectId,
            String name,
            String description) {

        Subject subject = subjectRepository.findByIdAndAdminId(subjectId, adminId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        boolean duplicate = subjectRepository
                .existsByAdminIdAndNameIgnoreCase(adminId, name.trim());

        if (duplicate && !subject.getName().equalsIgnoreCase(name.trim())) {
            throw new IllegalArgumentException("A subject with this name already exists");
        }

        subject.updateDetails(name, description);
        return subjectRepository.save(subject);
    }

    public void deleteSubject(Long adminId, Long subjectId) {
        Subject subject = subjectRepository.findByIdAndAdminId(subjectId, adminId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        subjectRepository.delete(subject);
    }
}