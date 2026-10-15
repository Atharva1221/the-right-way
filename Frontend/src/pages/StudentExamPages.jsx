import { useCallback, useEffect, useRef, useState } from "react";
import { useBlocker, useLocation, useNavigate, useParams } from "react-router-dom";
import { ApiError, apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
import { Alert, Button, ButtonLink, EmptyState, LoadingState } from "../components/ui";
import {
  attemptStorageKey,
  readAttempt,
  restoreAnswers,
  secondsRemaining,
} from "../student/attemptState";
import { PageHeading } from "./StudentDashboardPage";

function formatDate(date) {
  return date ? new Date(date).toLocaleString() : null;
}

function examUnavailableMessage(error) {
  if (error instanceof ApiError && error.status === 404) {
    return "This exam is no longer available. It may have been closed or removed.";
  }
  if (error instanceof ApiError && error.status === 403) {
    return "Your account does not have access to this exam.";
  }
  return error.message || "The exam could not be loaded.";
}

function Instructions({ exam, onStart, isStarting }) {
  return (
    <div className="student-page">
      <PageHeading
        eyebrow="Exam details"
        title={exam.title}
        description={exam.subjectName}
      />
      <section className="exam-instructions" aria-labelledby="instructions-heading">
        <h2 id="instructions-heading">Before you begin</h2>
        {exam.description && <p className="exam-instructions__description">{exam.description}</p>}
        <dl className="exam-facts">
          <div>
            <dt>Subject</dt>
            <dd>{exam.subjectName}</dd>
          </div>
          <div>
            <dt>Duration</dt>
            <dd>{exam.durationMinutes} minutes</dd>
          </div>
          <div>
            <dt>Questions</dt>
            <dd>{exam.questions.length}</dd>
          </div>
          {exam.startAt && (
            <div>
              <dt>Available from</dt>
              <dd>{formatDate(exam.startAt)}</dd>
            </div>
          )}
          {exam.endAt && (
            <div>
              <dt>Available until</dt>
              <dd>{formatDate(exam.endAt)}</dd>
            </div>
          )}
        </dl>
        <div className="instruction-note">
          <span className="instruction-note__mark" aria-hidden="true">i</span>
          <p>
            The timer starts when you choose “Start exam”. Your start time and selected options are saved in this browser tab for reload recovery, but not in your result history.
            Timing is client-side and cannot be guaranteed after browser storage is cleared. You can take the exam again later.
          </p>
        </div>
        <div className="exam-instructions__actions">
          <ButtonLink to="/student/exams" variant="quiet">Back to exams</ButtonLink>
          <Button disabled={isStarting || exam.questions.length === 0} onClick={onStart}>
            {isStarting ? "Starting…" : "Start exam"}
          </Button>
        </div>
      </section>
    </div>
  );
}

export function ExamInstructionsPage() {
  const { user } = useAuth();
  const { examId } = useParams();
  const navigate = useNavigate();
  const [exam, setExam] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isStarting, setIsStarting] = useState(false);
  const [storageWarning, setStorageWarning] = useState("");

  useEffect(() => {
    let active = true;
    setIsLoading(true);
    apiRequest(`/api/student/exams/${encodeURIComponent(examId)}`)
      .then((data) => {
        if (!data || !Array.isArray(data.questions)) {
          throw new Error("The server returned invalid exam details.");
        }
        if (active) {
          setExam(data);
          setError("");
        }
      })
      .catch((loadError) => {
        if (active) {
          setError(examUnavailableMessage(loadError));
        }
      })
      .finally(() => {
        if (active) {
          setIsLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, [examId]);

  function startExam() {
    if (!exam || isStarting) return;
    setIsStarting(true);
    const startedAt = Date.now();
    try {
      window.sessionStorage.setItem(
        attemptStorageKey(user.id, exam.id),
        JSON.stringify({
          userId: String(user.id),
          examId: String(exam.id),
          startedAt,
          answers: {},
        }),
      );
    } catch {
      setStorageWarning(
        "This browser could not save exam progress for reload recovery. Keep this page open until you submit.",
      );
    }
    navigate(`/student/exams/${exam.id}/take`, {
      state: { exam, startedAt },
    });
  }

  if (isLoading) {
    return <LoadingState label="Loading exam instructions" />;
  }
  if (error || !exam) {
    return (
      <div className="student-page">
        <PageHeading eyebrow="Exam details" title="Exam unavailable" />
        <Alert>{error || "The exam could not be loaded."}</Alert>
        <ButtonLink to="/student/exams" variant="quiet">Back to exams</ButtonLink>
      </div>
    );
  }
  return (
    <>
      {storageWarning && <Alert variant="warning">{storageWarning}</Alert>}
      <Instructions exam={exam} onStart={startExam} isStarting={isStarting} />
    </>
  );
}

function formatTime(seconds) {
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  const remainingSeconds = seconds % 60;
  return [hours, minutes, remainingSeconds]
    .map((part) => String(part).padStart(2, "0"))
    .join(":");
}

function ConfirmationDialog({ title, children, primaryLabel, onConfirm, onCancel, isBusy }) {
  const dialogRef = useRef(null);
  useEffect(() => {
    const dialog = dialogRef.current;
    dialog?.focus();
    function trapFocus(event) {
      if (event.key === "Escape") {
        event.preventDefault();
        if (!isBusy) onCancel();
        return;
      }
      if (event.key !== "Tab" || !dialog) return;
      const focusable = [...dialog.querySelectorAll("button:not(:disabled)")];
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
    dialog?.addEventListener("keydown", trapFocus);
    return () => dialog?.removeEventListener("keydown", trapFocus);
  }, [isBusy, onCancel]);

  return (
    <div className="dialog-backdrop">
      <section
        aria-labelledby="confirmation-title"
        aria-modal="true"
        className="confirmation-dialog"
        role="dialog"
        tabIndex="-1"
        ref={dialogRef}
      >
        <p className="eyebrow">Please confirm</p>
        <h2 id="confirmation-title">{title}</h2>
        <p>{children}</p>
        <div className="confirmation-dialog__actions">
          <Button disabled={isBusy} onClick={onCancel} variant="quiet">
            {isBusy ? "Submitting…" : "Go back"}
          </Button>
          <Button disabled={isBusy} onClick={onConfirm}>
            {isBusy ? "Submitting…" : primaryLabel}
          </Button>
        </div>
      </section>
    </div>
  );
}

export function ActiveExamPage() {
  const { user } = useAuth();
  const { examId } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const [attemptCache] = useState(() => readAttempt(
    { getItem: (key) => window.sessionStorage.getItem(key) },
    user.id,
    examId,
  ));
  const [exam, setExam] = useState(() =>
    String(location.state?.exam?.id) === String(examId)
      ? location.state.exam
      : null,
  );
  const [isLoadingExam, setIsLoadingExam] = useState(!exam);
  const [examLoadError, setExamLoadError] = useState("");
  const startedAt = attemptCache.attempt?.startedAt || Number(location.state?.startedAt);
  const questions = Array.isArray(exam?.questions) ? exam.questions : [];
  const durationSeconds = Math.max(0, Number(exam?.durationMinutes) * 60 || 0);

  const [currentIndex, setCurrentIndex] = useState(0);
  const [answers, setAnswers] = useState(() => attemptCache.attempt?.answers || {});
  const [remainingSeconds, setRemainingSeconds] = useState(() =>
    startedAt && durationSeconds > 0
      ? secondsRemaining(startedAt, durationSeconds)
      : durationSeconds,
  );
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submissionError, setSubmissionError] = useState("");
  const [showSubmitConfirmation, setShowSubmitConfirmation] = useState(false);
  const [autoSubmitFailed, setAutoSubmitFailed] = useState(false);
  const submittingRef = useRef(false);
  const completedRef = useRef(false);
  const autoSubmitAttemptedRef = useRef(false);
  const hydratedExamRef = useRef(null);
  const [storageWarning, setStorageWarning] = useState(attemptCache.warning);

  const loadExam = useCallback(async () => {
    setIsLoadingExam(true);
    setExamLoadError("");
    try {
      const response = await apiRequest(`/api/student/exams/${encodeURIComponent(examId)}`);
      if (!response || !Array.isArray(response.questions)) {
        throw new Error("The server returned invalid exam details.");
      }
      setExam(response);
    } catch (loadError) {
      setExamLoadError(examUnavailableMessage(loadError));
    } finally {
      setIsLoadingExam(false);
    }
  }, [examId]);

  useEffect(() => {
    if (!exam) {
      void loadExam();
    }
  }, [exam, loadExam]);

  useEffect(() => {
    if (!exam || hydratedExamRef.current === String(exam.id)) {
      return;
    }
    hydratedExamRef.current = String(exam.id);
    setAnswers(restoreAnswers(exam.questions, attemptCache.attempt?.answers));
  }, [attemptCache.attempt, exam]);

  useEffect(() => {
    if (!exam || !startedAt || completedRef.current) {
      return;
    }
    try {
      window.sessionStorage.setItem(
        attemptStorageKey(user.id, examId),
        JSON.stringify({
          userId: String(user.id),
          examId: String(examId),
          startedAt,
          answers,
        }),
      );
      setStorageWarning("");
    } catch {
      setStorageWarning(
        "Progress could not be saved for reload recovery. Your answers remain available while this page is open.",
      );
    }
  }, [answers, exam, examId, startedAt, user.id]);

  const shouldBlock = useCallback(
    ({ currentLocation, nextLocation }) =>
      startedAt > 0
      && !completedRef.current
      && currentLocation.pathname !== nextLocation.pathname,
    [startedAt],
  );
  const blocker = useBlocker(shouldBlock);

  const submitExam = useCallback(async (automatic = false) => {
    if (submittingRef.current || completedRef.current || !exam || questions.length === 0) {
      return;
    }
    submittingRef.current = true;
    setIsSubmitting(true);
    setSubmissionError("");
    setShowSubmitConfirmation(false);

    const answersPayload = questions.map((question) => ({
      questionId: question.id,
      selectedOptionId: question.options.some(
        (option) => String(option.id) === String(answers[String(question.id)]),
      )
        ? answers[String(question.id)]
        : null,
    }));

    try {
      const result = await apiRequest(`/api/student/exams/${encodeURIComponent(examId)}/submissions`, {
        method: "POST",
        body: JSON.stringify({ answers: answersPayload }),
      });
      if (
        !result
        || !Number.isFinite(Number(result.resultId))
        || !Number.isFinite(Number(result.score))
        || !Number.isFinite(Number(result.totalQuestions))
      ) {
        throw new Error("The server returned an invalid exam result.");
      }
      completedRef.current = true;
      try {
        window.sessionStorage.removeItem(attemptStorageKey(user.id, examId));
      } catch {
        setStorageWarning("The submitted exam progress could not be cleared from this browser tab.");
      }
      navigate(`/student/results/${examId}?resultId=${encodeURIComponent(result.resultId)}`, {
        replace: true,
        state: { submissionResult: result, examTitle: exam.title },
      });
    } catch (submitError) {
      setSubmissionError(submitError.message || "Your result could not be submitted.");
      setAutoSubmitFailed(automatic || remainingSeconds <= 0);
      submittingRef.current = false;
      setIsSubmitting(false);
    }
  }, [answers, exam, examId, navigate, questions, remainingSeconds, user.id]);

  useEffect(() => {
    if (!startedAt || durationSeconds <= 0 || completedRef.current) {
      return undefined;
    }

    const updateRemaining = () => {
      const seconds = secondsRemaining(startedAt, durationSeconds);
      setRemainingSeconds(seconds);
      if (seconds === 0 && !autoSubmitAttemptedRef.current) {
        autoSubmitAttemptedRef.current = true;
        void submitExam(true);
      }
    };

    updateRemaining();
    const timer = window.setInterval(updateRemaining, 1000);
    const updateWhenVisible = () => {
      if (document.visibilityState === "visible") {
        updateRemaining();
      }
    };
    window.addEventListener("focus", updateRemaining);
    window.addEventListener("pageshow", updateRemaining);
    document.addEventListener("visibilitychange", updateWhenVisible);
    return () => {
      window.clearInterval(timer);
      window.removeEventListener("focus", updateRemaining);
      window.removeEventListener("pageshow", updateRemaining);
      document.removeEventListener("visibilitychange", updateWhenVisible);
    };
  }, [durationSeconds, startedAt, submitExam]);

  useEffect(() => {
    if (!startedAt || completedRef.current) return undefined;
    const warnBeforeLeave = (event) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", warnBeforeLeave);
    return () => window.removeEventListener("beforeunload", warnBeforeLeave);
  }, [startedAt]);

  if (isLoadingExam) {
    return <LoadingState label="Restoring your exam" />;
  }
  if (examLoadError || !exam) {
    return (
      <div className="student-page">
        <PageHeading eyebrow="Active exam" title="Exam unavailable" />
        <Alert>{examLoadError || "The exam could not be restored."}</Alert>
        <Button onClick={() => void loadExam()} variant="quiet">Try again</Button>
        <ButtonLink to="/student/exams" variant="quiet">Back to exams</ButtonLink>
      </div>
    );
  }
  if (!startedAt || durationSeconds <= 0 || questions.length === 0) {
    return <NavigateToInstructions examId={examId} />;
  }

  const question = questions[currentIndex];
  const answeredCount = Object.keys(answers).length;
  const unansweredCount = questions.length - answeredCount;

  function selectOption(optionId) {
    setAnswers((current) => ({ ...current, [String(question.id)]: optionId }));
  }

  return (
    <div className="student-page exam-active">
      <div className="active-exam-heading">
        <div>
          <p className="eyebrow">{exam.subjectName}</p>
          <h1>{exam.title}</h1>
          <p>Question {currentIndex + 1} of {questions.length}</p>
        </div>
        <div className={`exam-timer${remainingSeconds <= 60 ? " exam-timer--urgent" : ""}`} role="timer" aria-live="off">
          <span>Time remaining</span>
          <strong>{formatTime(remainingSeconds)}</strong>
        </div>
      </div>

      {submissionError && (
        <Alert>
          {submissionError} Your answers are still here. You can try submitting again.
        </Alert>
      )}
      {storageWarning && <Alert variant="warning">{storageWarning}</Alert>}

      <div className="question-layout">
        <section className="question-panel" aria-labelledby={`question-${question.id}`}>
          <p className="question-panel__number">Question {currentIndex + 1}</p>
          <h2 id={`question-${question.id}`}>{question.questionText}</h2>
          <fieldset className="option-list">
            <legend className="sr-only">Choose an answer</legend>
            {question.options.map((option, index) => (
              <label className="option-choice" key={option.id}>
                <input
                  checked={answers[String(question.id)] === option.id}
                  disabled={remainingSeconds <= 0 || isSubmitting}
                  name={`question-${question.id}`}
                  onChange={() => selectOption(option.id)}
                  type="radio"
                  value={option.id}
                />
                <span className="option-choice__letter" aria-hidden="true">
                  {String.fromCharCode(65 + index)}
                </span>
                <span>{option.optionText}</span>
              </label>
            ))}
          </fieldset>
          <div className="question-controls">
            <Button
              disabled={currentIndex === 0}
              onClick={() => setCurrentIndex((index) => Math.max(0, index - 1))}
              variant="quiet"
            >
              Previous
            </Button>
            {currentIndex < questions.length - 1 ? (
              <Button onClick={() => setCurrentIndex((index) => Math.min(questions.length - 1, index + 1))}>
                Next question
              </Button>
            ) : (
              <Button
                disabled={isSubmitting}
                onClick={() => setShowSubmitConfirmation(true)}
              >
                Submit exam
              </Button>
            )}
          </div>
        </section>

        <aside className="question-nav" aria-label="Question navigator">
          <div className="question-nav__heading">
            <h2>Questions</h2>
            <span>{answeredCount} of {questions.length} answered</span>
          </div>
          <div className="question-nav__grid">
            {questions.map((item, index) => {
              const answered = answers[String(item.id)] !== undefined;
              const current = index === currentIndex;
              return (
                <button
                  aria-current={current ? "step" : undefined}
                  aria-label={`Question ${index + 1}, ${answered ? "answered" : "unanswered"}${current ? ", current" : ""}`}
                  className={[
                    "question-nav__item",
                    answered ? "question-nav__item--answered" : "",
                    current ? "question-nav__item--current" : "",
                  ].filter(Boolean).join(" ")}
                  key={item.id}
                  onClick={() => setCurrentIndex(index)}
                  type="button"
                >
                  {index + 1}
                </button>
              );
            })}
          </div>
          <div className="question-nav__legend">
            <span><i className="question-nav__dot question-nav__dot--answered" /> Answered</span>
            <span><i className="question-nav__dot" /> Unanswered</span>
          </div>
          <p className="question-nav__unanswered">
            {unansweredCount} {unansweredCount === 1 ? "question" : "questions"} unanswered
          </p>
          <Button
            className="question-nav__submit"
            disabled={isSubmitting}
            onClick={() => setShowSubmitConfirmation(true)}
          >
            Submit exam
          </Button>
          {autoSubmitFailed && (
            <Button
              className="question-nav__retry"
              disabled={isSubmitting}
              onClick={() => void submitExam()}
              variant="quiet"
            >
              Retry submission
            </Button>
          )}
        </aside>
      </div>

      {showSubmitConfirmation && (
        <ConfirmationDialog
          title="Submit this exam?"
          isBusy={isSubmitting}
          onCancel={() => setShowSubmitConfirmation(false)}
          onConfirm={() => void submitExam()}
          primaryLabel="Submit exam"
        >
          {unansweredCount > 0
            ? `${unansweredCount} ${unansweredCount === 1 ? "question is" : "questions are"} unanswered. You can change answers until submission.`
            : "You have answered every question. Your answers cannot be changed after submission."}
        </ConfirmationDialog>
      )}
      {blocker.state === "blocked" && (
        <ConfirmationDialog
          title="Leave this exam?"
          isBusy={isSubmitting}
          onCancel={() => blocker.reset()}
          onConfirm={() => blocker.proceed()}
          primaryLabel="Leave exam"
        >
          Your current answers are only held in this page. Leaving will discard them.
        </ConfirmationDialog>
      )}
    </div>
  );
}

function NavigateToInstructions({ examId }) {
  const navigate = useNavigate();
  useEffect(() => {
    navigate(`/student/exams/${encodeURIComponent(examId)}`, { replace: true });
  }, [examId, navigate]);
  return <LoadingState label="Opening exam instructions" />;
}
