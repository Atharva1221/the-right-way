package com.examplatform.question.controller;

import com.examplatform.user.enums.UserRole;
import com.examplatform.question.service.QuestionService;
import com.examplatform.user.entity.User;
import com.examplatform.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionControllerTest {

    @Mock
    private QuestionService questionService;

    @Mock
    private UserRepository userRepository;

    @Test
    void studentCannotAccessQuestionManagement() {
        QuestionController controller =
                new QuestionController(questionService, userRepository);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("student@example.test");
        when(userRepository.findByEmail("student@example.test"))
                .thenReturn(Optional.of(new User(
                        "Student",
                        "student@example.test",
                        "hash",
                        UserRole.STUDENT)));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.getQuestions(authentication, 12L));
        verifyNoInteractions(questionService);
    }
}
