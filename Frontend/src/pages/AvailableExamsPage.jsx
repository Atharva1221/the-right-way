import { useCallback, useEffect, useState } from "react";
import { ApiError, apiRequest } from "../api/client";
import { Alert, Button, ButtonLink, EmptyState, LoadingState } from "../components/ui";
import { PageHeading } from "./StudentDashboardPage";

function availabilityText(exam) {
  if (exam.endAt) {
    return `Available until ${new Date(exam.endAt).toLocaleString()}`;
  }
  if (exam.startAt) {
    return `Available from ${new Date(exam.startAt).toLocaleString()}`;
  }
  return "Available now";
}

export function AvailableExamsPage() {
  const [exams, setExams] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const loadExams = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await apiRequest("/api/student/exams");
      if (!Array.isArray(data)) {
        throw new Error("The server returned an invalid exam catalogue.");
      }
      setExams(data);
    } catch (loadError) {
      setError(loadError instanceof ApiError && loadError.status === 403
        ? "Your account does not have access to the student exam catalogue."
        : loadError.message || "Available exams could not be loaded.");
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadExams();
  }, [loadExams]);

  return (
    <div className="student-page">
      <PageHeading
        eyebrow="Practice"
        title="Available exams"
        description="Choose an exam to review its details and instructions before you begin."
      />

      {isLoading && <LoadingState label="Finding available exams" />}
      {!isLoading && error && (
        <>
          <Alert>{error}</Alert>
          <Button onClick={() => void loadExams()} variant="quiet">
            Try again
          </Button>
        </>
      )}
      {!isLoading && !error && exams?.length === 0 && (
        <EmptyState title="No exams are available right now">
          Published public exams appear here while they are within their availability window. Check back later.
        </EmptyState>
      )}
      {!isLoading && !error && exams?.length > 0 && (
        <section className="exam-list" aria-label="Available exams">
          {exams.map((exam) => (
            <article className="exam-row" key={exam.id}>
              <div className="exam-row__content">
                <p className="exam-row__subject">{exam.subjectName}</p>
                <h2>{exam.title}</h2>
                {exam.description && <p className="exam-row__description">{exam.description}</p>}
                <div className="exam-row__meta">
                  <span>{exam.durationMinutes} minutes</span>
                  <span aria-hidden="true">·</span>
                  <span>{availabilityText(exam)}</span>
                </div>
              </div>
              <ButtonLink to={`/student/exams/${exam.id}`}>
                View exam
              </ButtonLink>
            </article>
          ))}
        </section>
      )}
    </div>
  );
}
