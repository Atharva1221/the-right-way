import { useCallback, useEffect, useState } from "react";
import { useLocation, useParams, useSearchParams } from "react-router-dom";
import { ApiError, apiRequest } from "../api/client";
import { Button, ButtonLink, EmptyState, LoadingState } from "../components/ui";
import { PageHeading } from "./StudentDashboardPage";

function resultErrorMessage(error) {
  if (error instanceof ApiError && error.status === 404) {
    return "This result is not available for your account.";
  }
  if (error instanceof ApiError && error.status === 403) {
    return "You do not have permission to view this result.";
  }
  return error.message || "The result could not be loaded.";
}

function formatDate(date) {
  return date ? new Date(date).toLocaleString() : "Unavailable";
}

export function ExamResultPage() {
  const { examId } = useParams();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const resultId = searchParams.get("resultId");
  const submissionResult = location.state?.submissionResult;
  const submissionDetails = String(submissionResult?.resultId) === String(resultId)
    && String(submissionResult?.examId) === String(examId)
    ? submissionResult
    : null;
  const [result, setResult] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(Boolean(resultId));

  const loadResult = useCallback(async () => {
    if (!resultId) {
      setError("This result URL does not contain a result ID.");
      setIsLoading(false);
      return;
    }
    setIsLoading(true);
    setError("");
    try {
      const response = await apiRequest(`/api/student/results/${encodeURIComponent(resultId)}`);
      if (
        !response
        || String(response.resultId) !== String(resultId)
        || String(response.examId) !== String(examId)
        || !Number.isFinite(Number(response.score))
        || !Number.isFinite(Number(response.maximumMarks))
        || typeof response.passed !== "boolean"
      ) {
        throw new Error("The server returned an invalid result summary.");
      }
      setResult(response);
    } catch (loadError) {
      setError(resultErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }, [examId, resultId]);

  useEffect(() => {
    void loadResult();
  }, [loadResult]);

  if (isLoading) {
    return <LoadingState label="Loading your saved result" />;
  }

  if (error || !result) {
    return (
      <div className="student-page">
        <PageHeading
          eyebrow="Exam result"
          title="Result unavailable"
          description={error || "The result could not be loaded."}
        />
        {resultId && (
          <Button onClick={() => void loadResult()} variant="quiet">
            Try again
          </Button>
        )}
        <EmptyState title="Your dashboard keeps the summary">
          Your completed attempts are included in your student dashboard.
        </EmptyState>
        <div className="result-actions">
          <ButtonLink to="/student/dashboard">View dashboard</ButtonLink>
          <ButtonLink to="/student/exams" variant="quiet">Back to exams</ButtonLink>
        </div>
      </div>
    );
  }

  const passed = submissionDetails
    ? submissionDetails.passed
    : result.passed;
  const hasPassStatus = typeof passed === "boolean";
  const statusLabel = !hasPassStatus
    ? "Pass threshold not set"
    : passed
      ? "Passed"
      : "Not passed";
  const statusTone = !hasPassStatus ? "neutral" : passed ? "passed" : "failed";
  const passingMarks = submissionDetails?.passingMarks;
  const examTitle = location.state?.examTitle || result.examTitle;

  return (
    <div className="student-page">
      <PageHeading
        eyebrow="Submission complete"
        title={examTitle}
        description="Your saved result summary is available from this link."
      />

      <section className="result-summary" aria-labelledby="result-summary-heading">
        <div className={`result-summary__status result-summary__status--${statusTone}`}>
          <span className="result-summary__status-mark" aria-hidden="true">
            {!hasPassStatus ? "i" : passed ? "✓" : "−"}
          </span>
          <div>
            <span className="result-summary__status-label">Result</span>
            <strong>{statusLabel}</strong>
          </div>
        </div>
        <div className="result-summary__score">
          <p className="eyebrow">Final score</p>
          <h2 id="result-summary-heading">
            {result.score} <span>/ {result.maximumMarks}</span>
          </h2>
          {passingMarks !== undefined
            ? passingMarks === null
              ? <p>No passing threshold was configured for this exam.</p>
              : <p>Passing marks: {passingMarks}</p>
            : <p>Submitted {formatDate(result.submittedAt)}</p>}
        </div>
      </section>

      <section className="answer-summary" aria-labelledby="answer-summary-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Saved result</p>
            <h2 id="answer-summary-heading">Attempt summary</h2>
          </div>
          <span className="answer-summary__total">
            {result.subjectName} · {formatDate(result.submittedAt)}
          </span>
        </div>
        {submissionDetails ? (
          <dl className="answer-summary__grid">
            <div className="answer-summary__item answer-summary__item--green">
              <dt>Correct answers</dt>
              <dd>{submissionDetails.correctAnswers}</dd>
            </div>
            <div className="answer-summary__item answer-summary__item--red">
              <dt>Incorrect answers</dt>
              <dd>{submissionDetails.incorrectAnswers}</dd>
            </div>
            <div className="answer-summary__item">
              <dt>Unanswered</dt>
              <dd>{submissionDetails.unansweredQuestions}</dd>
            </div>
          </dl>
        ) : (
          <EmptyState title="Summary only">
            For privacy, individual answers and answer-by-answer history are not stored. This saved result contains your score, maximum marks, and pass status.
          </EmptyState>
        )}
      </section>

      <div className="result-actions">
        <ButtonLink to="/student/dashboard">View dashboard</ButtonLink>
        <ButtonLink to="/student/exams" variant="quiet">Back to exams</ButtonLink>
      </div>
    </div>
  );
}
