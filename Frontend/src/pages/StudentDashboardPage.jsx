import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { ApiError, apiRequest } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
import { Alert, Button, ButtonLink, EmptyState, LoadingState } from "../components/ui";

const emptyDashboard = {
  testsTaken: 0,
  testsPassed: 0,
  testsFailed: 0,
  passRate: 0,
  subjectPerformance: [],
};

const metrics = [
  { key: "testsTaken", label: "Tests taken", tone: "navy" },
  { key: "testsPassed", label: "Tests passed", tone: "green" },
  { key: "testsFailed", label: "Tests failed", tone: "red" },
  { key: "passRate", label: "Pass rate", tone: "blue", suffix: "%" },
];

function displayError(error) {
  if (error instanceof ApiError && error.status === 403) {
    return "You do not have permission to view these results.";
  }
  if (error instanceof ApiError && error.status === 404) {
    return "The dashboard is not available right now.";
  }
  return error.message || "The dashboard could not be loaded.";
}

export function StudentDashboardPage() {
  const { user } = useAuth();
  const [dashboard, setDashboard] = useState(null);
  const [error, setError] = useState("");
  const [isLoading, setIsLoading] = useState(true);

  const loadDashboard = useCallback(async () => {
    setIsLoading(true);
    setError("");
    try {
      const data = await apiRequest("/api/student/dashboard");
      if (
        !data
        || !Number.isFinite(Number(data.testsTaken))
        || !Number.isFinite(Number(data.testsPassed))
        || !Number.isFinite(Number(data.testsFailed))
        || !Number.isFinite(Number(data.passRate))
        || !Array.isArray(data.subjectPerformance)
      ) {
        throw new Error("The server returned an invalid dashboard response.");
      }
      setDashboard(data);
    } catch (loadError) {
      setError(displayError(loadError));
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, [loadDashboard]);

  if (isLoading && !dashboard) {
    return <LoadingState label="Loading your performance" />;
  }

  if (error && !dashboard) {
    return (
      <div className="student-page">
        <PageHeading
          eyebrow="Your preparation"
          title={`Welcome, ${user?.name?.split(" ")[0] || "student"}`}
          description="A clear view of your practice and progress."
        />
        <Alert>{error}</Alert>
        <Button onClick={() => void loadDashboard()} variant="quiet">
          Try again
        </Button>
      </div>
    );
  }

  const current = dashboard || emptyDashboard;
  const noAttempts = Number(current.testsTaken) === 0;

  return (
    <div className="student-page">
      <PageHeading
        eyebrow="Your preparation"
        title={`Welcome, ${user?.name?.split(" ")[0] || "student"}`}
        description="A clear view of your practice and progress."
      />

      {error && <Alert variant="warning">{error}</Alert>}

      <section className="metric-grid" aria-label="Performance summary">
        {metrics.map((metric) => {
          const value = Number(current[metric.key]);
          return (
            <div className="metric" key={metric.key}>
              <span className="metric__label">{metric.label}</span>
              <strong className={`metric__value metric__value--${metric.tone}`}>
                {metric.key === "passRate" ? value.toFixed(2) : value}
                {metric.suffix || ""}
              </strong>
            </div>
          );
        })}
      </section>

      <section className="subject-section" aria-labelledby="subject-performance-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">By subject</p>
            <h2 id="subject-performance-heading">Performance by subject</h2>
          </div>
          <ButtonLink to="/student/exams" variant="quiet">
            Find an exam
          </ButtonLink>
        </div>

        {noAttempts ? (
          <EmptyState title="Your practice starts here">
            Once you complete an exam, your attempts and subject performance will be summarized here.
          </EmptyState>
        ) : current.subjectPerformance.length === 0 ? (
          <EmptyState title="No subject results yet">
            Your completed attempts are recorded, but there is no subject breakdown to show.
          </EmptyState>
        ) : (
          <div className="subject-list">
            {current.subjectPerformance.map((subject) => {
              const percentage = Number(subject.averageScorePercentage);
              const progress = Number.isFinite(percentage)
                ? Math.max(0, Math.min(100, percentage))
                : 0;
              return (
                <article className="subject-row" key={subject.subjectId}>
                  <div className="subject-row__main">
                    <div className="subject-row__title">
                      <h3>{subject.subjectName}</h3>
                      <span>{subject.testsTaken} {subject.testsTaken === 1 ? "attempt" : "attempts"}</span>
                    </div>
                    <div
                      className="progress-track"
                      role="progressbar"
                      aria-label={`${subject.subjectName} average score`}
                      aria-valuemin="0"
                      aria-valuemax="100"
                      aria-valuenow={progress}
                    >
                      <span className="progress-track__fill" style={{ width: `${progress}%` }} />
                    </div>
                    <div className="subject-row__detail">
                      <span>
                        {subject.testsPassed} passed <span aria-hidden="true">·</span> {subject.testsFailed} failed
                      </span>
                      <strong>{percentage.toFixed(2)}%</strong>
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

export function PageHeading({ eyebrow, title, description, action }) {
  return (
    <div className="page-heading page-heading--student">
      <div>
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        {description && <p className="page-heading__description">{description}</p>}
      </div>
      {action}
    </div>
  );
}
