export function attemptStorageKey(userId, examId) {
  return `student-exam-attempt:${userId}:${examId}`;
}

export function readAttempt(storage, userId, examId) {
  try {
    const serialized = storage.getItem(attemptStorageKey(userId, examId));
    if (!serialized) {
      return { attempt: null, warning: "" };
    }
    const attempt = JSON.parse(serialized);
    if (
      !attempt
      || String(attempt.userId) !== String(userId)
      || String(attempt.examId) !== String(examId)
      || !Number.isSafeInteger(attempt.startedAt)
      || attempt.startedAt <= 0
      || !attempt.answers
      || typeof attempt.answers !== "object"
      || Array.isArray(attempt.answers)
    ) {
      return {
        attempt: null,
        warning: "Saved exam progress could not be validated. Starting again may not restore your previous answers.",
      };
    }
    return { attempt, warning: "" };
  } catch {
    return {
      attempt: null,
      warning: "This browser could not restore saved exam progress. Your current answers will remain available while this page is open.",
    };
  }
}

export function restoreAnswers(questions, savedAnswers) {
  const answers = {};
  for (const question of questions) {
    const selectedOptionId = savedAnswers?.[String(question.id)];
    if (
      selectedOptionId !== undefined
      && question.options.some((option) => String(option.id) === String(selectedOptionId))
    ) {
      answers[String(question.id)] = selectedOptionId;
    }
  }
  return answers;
}

export function secondsRemaining(startedAt, durationSeconds, now = Date.now()) {
  if (!Number.isFinite(startedAt) || startedAt <= 0 || !Number.isFinite(durationSeconds)) {
    return 0;
  }
  return Math.max(0, Math.ceil((startedAt + durationSeconds * 1000 - now) / 1000));
}
