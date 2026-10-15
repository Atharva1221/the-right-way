import { useCallback, useEffect, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { ApiError, apiRequest } from "../api/client";
import { Alert, Button, ButtonLink, EmptyState, LoadingState } from "../components/ui";
import { PageHeading } from "./StudentDashboardPage";
import { updateQuestionOptions } from "../admin/questionForm";

function errorMessage(error) {
  if (error instanceof ApiError && error.status === 401) {
    return "Your administrator session has expired. Sign in again.";
  }
  if (error instanceof ApiError && error.status === 403) {
    return "Your account is not allowed to manage this record.";
  }
  return error.message || "The request could not be completed.";
}

function useAdminData(loader, dependencies = []) {
  const [data, setData] = useState(null);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setData(await loader());
    } catch (loadError) {
      setError(errorMessage(loadError));
    } finally {
      setLoading(false);
    }
  // Loader dependencies are declared by the caller.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, dependencies);

  useEffect(() => {
    void load();
  }, [load]);

  return { data, error, loading, load, setData };
}

function AdminPage({ eyebrow, title, description, actions, children }) {
  return (
    <div className="admin-page">
      <div className="admin-page__heading">
        <PageHeading eyebrow={eyebrow} title={title} description={description} />
        {actions && <div className="admin-page__actions">{actions}</div>}
      </div>
      {children}
    </div>
  );
}

function StatusBadge({ children }) {
  return <span className={`admin-status admin-status--${String(children).toLowerCase()}`}>{children}</span>;
}

function dateLabel(value) {
  return value ? new Date(value).toLocaleDateString() : "No date limit";
}

function dateTimeInput(value) {
  if (!value) return "";
  const date = new Date(value);
  return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
    .toISOString()
    .slice(0, 16);
}

function dateTimeValue(value) {
  return value ? new Date(value).toISOString().slice(0, 19) : null;
}

export function AdminDashboardPage() {
  const { data, error, loading, load } = useAdminData(async () => {
    const [subjects, exams] = await Promise.all([
      apiRequest("/api/subjects"),
      apiRequest("/api/exams"),
    ]);
    if (!Array.isArray(subjects) || !Array.isArray(exams)) {
      throw new Error("The server returned unexpected admin data.");
    }
    return { subjects, exams };
  });

  if (loading && !data) return <LoadingState label="Loading administrator workspace" />;
  if (error && !data) {
    return (
      <AdminPage eyebrow="Administrator workspace" title="Overview" description="Manage subjects, exams, and questions.">
        <Alert>{error}</Alert>
        <Button onClick={() => void load()} variant="quiet">Try again</Button>
      </AdminPage>
    );
  }

  const current = data || { subjects: [], exams: [] };
  const counts = {
    subjects: current.subjects.length,
    exams: current.exams.length,
    published: current.exams.filter((exam) => exam.status === "PUBLISHED").length,
    drafts: current.exams.filter((exam) => exam.status === "DRAFT").length,
    archived: current.exams.filter((exam) => exam.status === "ARCHIVED").length,
  };

  return (
    <AdminPage
      eyebrow="Admin workspace"
      title="Your workspace"
      description="A current view of your subjects and exam library."
      actions={<Button onClick={() => void load()} variant="quiet">Refresh</Button>}
    >
      {error && <Alert variant="warning">{error}</Alert>}
      <section className="admin-metrics" aria-label="Workspace totals">
        {[
          ["Subjects", counts.subjects],
          ["Total exams", counts.exams],
          ["Published", counts.published],
          ["Drafts", counts.drafts],
          ["Archived", counts.archived],
        ].map(([label, value]) => (
          <div className="admin-metric" key={label}>
            <span>{label}</span><strong>{value}</strong>
          </div>
        ))}
      </section>
      <section className="admin-section" aria-labelledby="recent-exams-heading">
        <div className="admin-section__heading">
          <div>
            <p className="eyebrow">Exam library</p>
            <h2 id="recent-exams-heading">Recent exams</h2>
          </div>
          <ButtonLink to="/admin/exams" variant="quiet">Manage exams</ButtonLink>
        </div>
        {current.exams.length === 0 ? (
          <EmptyState title="No exams yet">Create an exam after adding a subject.</EmptyState>
        ) : (
          <div className="admin-table-wrap">
            <table className="admin-table">
              <thead><tr><th>Exam</th><th>Subject</th><th>Status</th><th>Visibility</th><th /></tr></thead>
              <tbody>{current.exams.slice(0, 6).map((exam) => (
                <tr key={exam.id}>
                  <td><strong>{exam.title}</strong><small>{exam.durationMinutes} minutes · created {dateLabel(exam.createdAt)}</small></td>
                  <td>{exam.subjectName}</td>
                  <td><StatusBadge>{exam.status}</StatusBadge></td>
                  <td>{exam.visibility === "PUBLIC_LINK" ? "Public link" : "Private"}</td>
                  <td><Link className="admin-inline-link" to={`/admin/exams/${exam.id}`}>Open</Link></td>
                </tr>
              ))}</tbody>
            </table>
          </div>
        )}
      </section>
      <div className="admin-shortcuts">
        <ButtonLink to="/admin/subjects" variant="quiet">Manage subjects</ButtonLink>
        <ButtonLink to="/admin/exams">Create an exam</ButtonLink>
      </div>
    </AdminPage>
  );
}

function SubjectForm({ subject, onCancel, onSaved }) {
  const [name, setName] = useState(subject?.name || "");
  const [description, setDescription] = useState(subject?.description || "");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  async function submit(event) {
    event.preventDefault();
    setError("");
    setSaving(true);
    try {
      const id = subject ? `/${subject.id}` : "";
      await apiRequest(`/api/subjects${id}`, {
        method: subject ? "PUT" : "POST",
        body: JSON.stringify({ name: name.trim(), description: description.trim() || null }),
      });
      onSaved(subject ? "Subject updated." : "Subject created.");
    } catch (saveError) {
      setError(errorMessage(saveError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="admin-form" onSubmit={submit}>
      {error && <Alert>{error}</Alert>}
      <label className="admin-field">Subject name
        <input required maxLength={100} value={name} onChange={(event) => setName(event.target.value)} />
      </label>
      <label className="admin-field">Description
        <textarea maxLength={500} rows={3} value={description} onChange={(event) => setDescription(event.target.value)} />
      </label>
      <div className="admin-form__actions">
        <Button type="submit" disabled={saving}>{saving ? "Saving…" : subject ? "Save changes" : "Create subject"}</Button>
        <Button variant="quiet" onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}

export function AdminSubjectsPage() {
  const { data, error, loading, load } = useAdminData(() => apiRequest("/api/subjects"));
  const [editing, setEditing] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [notice, setNotice] = useState("");
  const [actionError, setActionError] = useState("");

  async function deleteSubject(subject) {
    if (!window.confirm(`Delete "${subject.name}"? Subjects used by exams cannot be removed.`)) return;
    setActionError("");
    try {
      await apiRequest(`/api/subjects/${subject.id}`, { method: "DELETE" });
      setNotice("Subject deleted.");
      await load();
    } catch (deleteError) {
      setActionError(errorMessage(deleteError));
    }
  }

  function saved(message) {
    setShowForm(false);
    setEditing(null);
    setNotice(message);
    void load();
  }

  if (loading && !data) return <LoadingState label="Loading subjects" />;
  return (
    <AdminPage
      eyebrow="Learning structure"
      title="Subjects"
      description="Organize exams under the subjects you manage."
      actions={!showForm && <Button onClick={() => { setNotice(""); setShowForm(true); }}>Add subject</Button>}
    >
      {error && <Alert>{error}</Alert>}
      {actionError && <Alert>{actionError}</Alert>}
      {notice && <Alert variant="success">{notice}</Alert>}
      {(showForm || editing) && (
        <section className="admin-editor" aria-label={editing ? "Edit subject" : "Create subject"}>
          <h2>{editing ? "Edit subject" : "New subject"}</h2>
          <SubjectForm subject={editing} onCancel={() => { setEditing(null); setShowForm(false); }} onSaved={saved} />
        </section>
      )}
      {!error && data?.length === 0 ? (
        <EmptyState title="No subjects yet">Create a subject before creating its exams.</EmptyState>
      ) : (
        <div className="admin-card-list">
          {(data || []).map((subject) => (
            <article className="admin-record" key={subject.id}>
              <div><h2>{subject.name}</h2><p>{subject.description || "No description"} · Created {dateLabel(subject.createdAt)}</p></div>
              <div className="admin-record__actions">
                <Button variant="quiet" onClick={() => { setShowForm(false); setEditing(subject); setNotice(""); }}>Edit</Button>
                <Button variant="quiet" onClick={() => void deleteSubject(subject)}>Delete</Button>
              </div>
            </article>
          ))}
        </div>
      )}
    </AdminPage>
  );
}

const blankExam = {
  title: "", description: "", subjectId: "", durationMinutes: "30",
  passingMarks: "", shuffleQuestions: false, visibility: "PRIVATE", startAt: "", endAt: "",
};

function ExamForm({ exam, subjects, onSaved, onCancel }) {
  const [values, setValues] = useState(exam ? {
    title: exam.title,
    description: exam.description || "",
    subjectId: String(exam.subjectId),
    durationMinutes: String(exam.durationMinutes),
    passingMarks: exam.passingMarks ?? "",
    shuffleQuestions: Boolean(exam.shuffleQuestions),
    visibility: exam.visibility,
    startAt: dateTimeInput(exam.startAt),
    endAt: dateTimeInput(exam.endAt),
  } : blankExam);
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);
  const published = exam?.status === "PUBLISHED";

  function update(key, value) {
    setValues((current) => ({ ...current, [key]: value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setSaving(true);
    const request = {
      title: values.title.trim(),
      description: values.description.trim() || null,
      subjectId: Number(values.subjectId),
      durationMinutes: Number(values.durationMinutes),
      passingMarks: values.passingMarks === "" ? null : Number(values.passingMarks),
      shuffleQuestions: Boolean(values.shuffleQuestions),
      visibility: values.visibility,
      startAt: dateTimeValue(values.startAt),
      endAt: dateTimeValue(values.endAt),
    };
    try {
      await apiRequest(`/api/exams${exam ? `/${exam.id}` : ""}`, {
        method: exam ? "PUT" : "POST",
        body: JSON.stringify(request),
      });
      onSaved(exam ? "Exam updated." : "Exam created as a draft.");
    } catch (saveError) {
      setError(errorMessage(saveError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="admin-form admin-form--grid" onSubmit={submit}>
      {error && <Alert>{error}</Alert>}
      <label className="admin-field admin-field--wide">Title
        <input required maxLength={150} value={values.title} disabled={published} onChange={(e) => update("title", e.target.value)} />
      </label>
      <label className="admin-field admin-field--wide">Description
        <textarea rows={3} maxLength={1000} value={values.description} disabled={published} onChange={(e) => update("description", e.target.value)} />
      </label>
      <label className="admin-field">Subject
        <select required value={values.subjectId} disabled={published} onChange={(e) => update("subjectId", e.target.value)}>
          <option value="">Choose a subject</option>
          {subjects.map((subject) => <option key={subject.id} value={subject.id}>{subject.name}</option>)}
        </select>
      </label>
      <label className="admin-field">Duration (minutes)
        <input required type="number" min="1" value={values.durationMinutes} disabled={published} onChange={(e) => update("durationMinutes", e.target.value)} />
      </label>
      <label className="admin-field">Passing marks (optional)
        <input type="number" min="0" step="0.01" value={values.passingMarks} disabled={published} onChange={(e) => update("passingMarks", e.target.value)} />
      </label>
      <label className="admin-field">Visibility
        <select value={values.visibility} onChange={(e) => update("visibility", e.target.value)}>
          <option value="PRIVATE">Private</option>
          <option value="PUBLIC_LINK">Public link</option>
        </select>
      </label>
      {!published && (
        <>
          <label className="admin-field">Available from
            <input type="datetime-local" value={values.startAt} onChange={(e) => update("startAt", e.target.value)} />
          </label>
          <label className="admin-field">Available until
            <input type="datetime-local" value={values.endAt} onChange={(e) => update("endAt", e.target.value)} />
          </label>
          <label className="admin-check admin-field--wide">
            <input type="checkbox" checked={values.shuffleQuestions} onChange={(e) => update("shuffleQuestions", e.target.checked)} />
            Shuffle questions for students
          </label>
        </>
      )}
      {published && <p className="admin-form__note admin-field--wide">Published exam details are locked by the backend; visibility is the only editable setting.</p>}
      <div className="admin-form__actions admin-field--wide">
        <Button type="submit" disabled={saving || subjects.length === 0}>{saving ? "Saving…" : exam ? "Save changes" : "Create draft"}</Button>
        <Button variant="quiet" onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}

export function AdminExamsPage() {
  const { data: exams, error, loading, load } = useAdminData(() => apiRequest("/api/exams"));
  const {
    data: subjects,
    error: subjectsError,
    loading: subjectsLoading,
  } = useAdminData(() => apiRequest("/api/subjects"));
  const [showForm, setShowForm] = useState(false);
  const [notice, setNotice] = useState("");

  function saved(message) {
    setShowForm(false);
    setNotice(message);
    void load();
  }

  if (loading && !exams) return <LoadingState label="Loading exams" />;
  return (
    <AdminPage
      eyebrow="Exam library"
      title="Exams"
      description="Review status, control availability, and manage exam content."
      actions={!showForm && (
        <Button
          disabled={subjectsLoading || !subjects?.length}
          onClick={() => { setNotice(""); setShowForm(true); }}
        >
          Create exam
        </Button>
      )}
    >
      {error && <Alert>{error}</Alert>}
      {subjectsError && <Alert>{`Subjects could not be loaded: ${subjectsError}`}</Alert>}
      {notice && <Alert variant="success">{notice}</Alert>}
      {showForm && (
        <section className="admin-editor" aria-label="Create exam">
          <h2>New exam draft</h2>
          {subjectsLoading ? (
            <LoadingState label="Loading subjects" />
          ) : subjectsError ? (
            <Alert>{`Subjects could not be loaded: ${subjectsError}`}</Alert>
          ) : subjects?.length ? (
            <ExamForm subjects={subjects} onCancel={() => setShowForm(false)} onSaved={saved} />
          ) : <EmptyState title="Create a subject first">An exam must belong to one of your subjects.</EmptyState>}
        </section>
      )}
      {!error && exams?.length === 0 ? (
        <EmptyState title="No exams yet">Create your first exam draft to start adding questions.</EmptyState>
      ) : (
        <div className="admin-table-wrap">
          <table className="admin-table">
            <thead><tr><th>Exam</th><th>Subject</th><th>Status</th><th>Visibility</th><th>Duration</th><th /></tr></thead>
            <tbody>{(exams || []).map((exam) => (
              <tr key={exam.id}>
                <td><strong>{exam.title}</strong><small>{exam.description || "No description"}</small></td>
                <td>{exam.subjectName}</td>
                <td><StatusBadge>{exam.status}</StatusBadge></td>
                <td>{exam.visibility === "PUBLIC_LINK" ? "Public link" : "Private"}</td>
                <td>{exam.durationMinutes} min</td>
                <td><Link className="admin-inline-link" to={`/admin/exams/${exam.id}`}>Manage</Link></td>
              </tr>
            ))}</tbody>
          </table>
        </div>
      )}
    </AdminPage>
  );
}

const blankQuestion = {
  questionText: "", explanation: "", questionOrder: "", marks: "1", negativeMarks: "0",
  options: [
    { optionText: "", correct: true, optionOrder: 1 },
    { optionText: "", correct: false, optionOrder: 2 },
  ],
};

function QuestionForm({ examId, question, nextOrder, onSaved, onCancel }) {
  const [values, setValues] = useState(question ? {
    questionText: question.questionText,
    explanation: question.explanation || "",
    questionOrder: String(question.questionOrder),
    marks: String(question.marks),
    negativeMarks: String(question.negativeMarks),
    options: question.options.map((option) => ({
      optionText: option.optionText,
      correct: option.correct,
      optionOrder: option.optionOrder,
    })),
  } : { ...blankQuestion, questionOrder: String(nextOrder) });
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  function updateOption(index, key, value) {
    setValues((current) => ({
      ...current,
      options: updateQuestionOptions(current.options, index, key, value),
    }));
  }

  function addOption() {
    setValues((current) => ({
      ...current,
      options: [...current.options, {
        optionText: "",
        correct: false,
        optionOrder: current.options.length + 1,
      }],
    }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    if (values.options.filter((option) => option.correct).length !== 1) {
      setError("Mark exactly one option as correct.");
      return;
    }
    setSaving(true);
    const payload = {
      questionText: values.questionText.trim(),
      explanation: values.explanation.trim() || null,
      questionOrder: Number(values.questionOrder),
      marks: Number(values.marks),
      negativeMarks: Number(values.negativeMarks),
      options: values.options.map((option, index) => ({
        optionText: option.optionText.trim(),
        correct: option.correct,
        optionOrder: index + 1,
      })),
    };
    try {
      await apiRequest(
        question
          ? `/api/exams/${encodeURIComponent(examId)}/questions/${encodeURIComponent(question.id)}`
          : `/api/exams/${encodeURIComponent(examId)}/questions`,
        {
        method: question ? "PUT" : "POST",
        body: JSON.stringify(payload),
        },
      );
      onSaved();
    } catch (saveError) {
      setError(errorMessage(saveError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="admin-form" onSubmit={submit}>
      {error && <Alert>{error}</Alert>}
      <label className="admin-field">Question
        <textarea required rows={3} value={values.questionText} onChange={(e) => setValues({ ...values, questionText: e.target.value })} />
      </label>
      <div className="admin-form--grid">
        <label className="admin-field">Question order
          <input required type="number" min="1" value={values.questionOrder} onChange={(e) => setValues({ ...values, questionOrder: e.target.value })} />
        </label>
        <label className="admin-field">Marks
          <input required type="number" min="0" step="0.01" value={values.marks} onChange={(e) => setValues({ ...values, marks: e.target.value })} />
        </label>
        <label className="admin-field">Negative marks
          <input required type="number" min="0" step="0.01" value={values.negativeMarks} onChange={(e) => setValues({ ...values, negativeMarks: e.target.value })} />
        </label>
      </div>
      <label className="admin-field">Explanation (shown only after submission)
        <textarea rows={2} value={values.explanation} onChange={(e) => setValues({ ...values, explanation: e.target.value })} />
      </label>
      <fieldset className="admin-options">
        <legend>Answer options <span>· mark exactly one as correct</span></legend>
        {values.options.map((option, index) => (
          <div className="admin-option" key={index}>
            <label className="admin-option__text">Option {index + 1}
              <input required value={option.optionText} onChange={(e) => updateOption(index, "optionText", e.target.value)} />
            </label>
            <label className="admin-check">
              <input type="radio" name="correct-option" checked={option.correct} onChange={() => updateOption(index, "correct", true)} />
              Correct
            </label>
            {values.options.length > 2 && (
              <Button variant="quiet" onClick={() => setValues((current) => ({
                ...current,
                options: current.options.filter((_, i) => i !== index).map((item, i) => ({ ...item, optionOrder: i + 1 })),
              }))}>Remove</Button>
            )}
          </div>
        ))}
        <Button variant="quiet" onClick={addOption}>Add option</Button>
      </fieldset>
      <div className="admin-form__actions">
        <Button type="submit" disabled={saving}>{saving ? "Saving…" : "Save question"}</Button>
        <Button variant="quiet" onClick={onCancel}>Cancel</Button>
      </div>
    </form>
  );
}

export function AdminExamDetailPage() {
  const { examId } = useParams();
  const navigate = useNavigate();
  const { data, error, loading, load } = useAdminData(async () => {
    const [exam, questions] = await Promise.all([
      apiRequest(`/api/exams/${encodeURIComponent(examId)}`),
      apiRequest(`/api/exams/${encodeURIComponent(examId)}/questions`),
    ]);
    if (!Array.isArray(questions)) throw new Error("The server returned an invalid question list.");
    return { exam, questions };
  }, [examId]);
  const [subjects, setSubjects] = useState([]);
  const [editingExam, setEditingExam] = useState(false);
  const [editingQuestion, setEditingQuestion] = useState(null);
  const [addingQuestion, setAddingQuestion] = useState(false);
  const [notice, setNotice] = useState("");
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    apiRequest("/api/subjects").then(setSubjects).catch((loadError) => setActionError(errorMessage(loadError)));
  }, []);

  async function runAction(path, method, success) {
    setActionError("");
    setNotice("");
    try {
      await apiRequest(path, { method });
      setNotice(success);
      await load();
    } catch (actionFailure) {
      setActionError(errorMessage(actionFailure));
    }
  }

  async function deleteDraft() {
    if (!window.confirm(`Delete draft "${exam.title}" and its questions? This cannot be undone.`)) return;
    setActionError("");
    setNotice("");
    try {
      await apiRequest(`/api/exams/${encodeURIComponent(examId)}`, { method: "DELETE" });
      navigate("/admin/exams");
    } catch (deleteFailure) {
      setActionError(errorMessage(deleteFailure));
    }
  }

  async function deleteQuestion(question) {
    if (!window.confirm(`Delete question ${question.questionOrder}?`)) return;
    await runAction(`/api/exams/${examId}/questions/${question.id}`, "DELETE", "Question deleted.");
  }

  async function saveQuestion() {
    setAddingQuestion(false);
    setEditingQuestion(null);
    setNotice("Question saved.");
    await load();
  }

  if (loading && !data) return <LoadingState label="Loading exam details" />;
  if (error && !data) {
    return (
      <AdminPage eyebrow="Exam management" title="Exam unavailable" description={error}>
        <ButtonLink to="/admin/exams" variant="quiet">Back to exams</ButtonLink>
        <Button onClick={() => void load()} variant="quiet">Try again</Button>
      </AdminPage>
    );
  }

  const { exam, questions } = data;
  const draft = exam.status === "DRAFT";
  return (
    <AdminPage
      eyebrow="Exam management"
      title={exam.title}
      description={`${exam.subjectName} · ${exam.durationMinutes} minutes`}
      actions={<ButtonLink to="/admin/exams" variant="quiet">All exams</ButtonLink>}
    >
      {error && <Alert variant="warning">{error}</Alert>}
      {actionError && <Alert>{actionError}</Alert>}
      {notice && <Alert variant="success">{notice}</Alert>}
      <section className="admin-exam-summary">
        <div><span>Status</span><StatusBadge>{exam.status}</StatusBadge></div>
        <div><span>Visibility</span><strong>{exam.visibility === "PUBLIC_LINK" ? "Public link" : "Private"}</strong></div>
        <div><span>Questions</span><strong>{questions.length}</strong></div>
        <div><span>Passing marks</span><strong>{exam.passingMarks ?? "Not set"}</strong></div>
      </section>
      <section className="admin-section">
        <div className="admin-section__heading">
          <div><p className="eyebrow">Exam settings</p><h2>Details and availability</h2></div>
          {!editingExam && <Button variant="quiet" onClick={() => setEditingExam(true)}>Edit exam</Button>}
        </div>
        {editingExam ? (
          <ExamForm
            exam={exam}
            subjects={subjects}
            onCancel={() => setEditingExam(false)}
            onSaved={async (message) => { setEditingExam(false); setNotice(message); await load(); }}
          />
        ) : (
          <>
            {exam.description && <p className="admin-description">{exam.description}</p>}
            <dl className="admin-facts">
              <div><dt>Available from</dt><dd>{exam.startAt ? new Date(exam.startAt).toLocaleString() : "Immediately"}</dd></div>
              <div><dt>Available until</dt><dd>{exam.endAt ? new Date(exam.endAt).toLocaleString() : "No end date"}</dd></div>
              <div><dt>Shuffle questions</dt><dd>{exam.shuffleQuestions ? "Yes" : "No"}</dd></div>
            </dl>
          </>
        )}
        <div className="admin-form__actions">
          {draft && <Button onClick={() => void runAction(`/api/exams/${examId}/publish`, "POST", "Exam published.")}>Publish exam</Button>}
          {exam.status !== "ARCHIVED" && <Button variant="quiet" onClick={() => {
            if (window.confirm(`Archive "${exam.title}"? Students will no longer be able to take it.`)) {
              void runAction(`/api/exams/${examId}/archive`, "POST", "Exam archived.");
            }
          }}>Archive</Button>}
          {draft && <Button variant="quiet" onClick={() => void deleteDraft()}>Delete draft</Button>}
        </div>
      </section>
      <section className="admin-section">
        <div className="admin-section__heading">
          <div><p className="eyebrow">Question bank</p><h2>Questions and options</h2></div>
          {draft && !addingQuestion && <Button onClick={() => { setAddingQuestion(true); setEditingQuestion(null); }}>Add question</Button>}
        </div>
        {addingQuestion && (
          <div className="admin-editor">
            <h3>New question</h3>
            <QuestionForm
              examId={exam.id}
              nextOrder={questions.length + 1}
              onCancel={() => setAddingQuestion(false)}
              onSaved={() => void saveQuestion()}
            />
          </div>
        )}
        {questions.length === 0 ? (
          <EmptyState title="No questions yet">Add at least one valid question before publishing this exam.</EmptyState>
        ) : (
          <div className="admin-question-list">
            {questions.map((question) => (
              <article className="admin-question" key={question.id}>
                {editingQuestion?.id === question.id ? (
                  <>
                    <h3>Edit question {question.questionOrder}</h3>
                    <QuestionForm
                      examId={exam.id}
                      question={question}
                      onCancel={() => setEditingQuestion(null)}
                      onSaved={() => void saveQuestion()}
                    />
                  </>
                ) : (
                  <>
                    <div className="admin-question__heading">
                      <div><span>Question {question.questionOrder} · {question.marks} marks · −{question.negativeMarks} negative</span><h3>{question.questionText}</h3></div>
                      {draft && <div className="admin-record__actions">
                        <Button variant="quiet" onClick={() => { setAddingQuestion(false); setEditingQuestion(question); }}>Edit</Button>
                        <Button variant="quiet" onClick={() => void deleteQuestion(question)}>Delete</Button>
                      </div>}
                    </div>
                    <ul className="admin-answer-options">
                      {question.options.map((option) => (
                        <li key={option.id} className={option.correct ? "admin-answer-options__correct" : ""}>
                          <span>{option.optionOrder}.</span> {option.optionText}
                          {option.correct && <strong>Correct answer</strong>}
                        </li>
                      ))}
                    </ul>
                    {question.explanation && <p className="admin-question__explanation">Explanation: {question.explanation}</p>}
                  </>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
    </AdminPage>
  );
}
