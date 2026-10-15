import assert from "node:assert/strict";
import test from "node:test";
import {
  attemptStorageKey,
  readAttempt,
  restoreAnswers,
  secondsRemaining,
} from "./attemptState.js";

test("attempt storage keys are scoped to both student and exam", () => {
  assert.equal(attemptStorageKey(7, 12), "student-exam-attempt:7:12");
  assert.notEqual(attemptStorageKey(7, 12), attemptStorageKey(8, 12));
});

test("restores only saved answers belonging to the current questions", () => {
  const questions = [
    { id: 1, options: [{ id: 10 }, { id: 11 }] },
    { id: 2, options: [{ id: 20 }] },
  ];

  assert.deepEqual(
    restoreAnswers(questions, { "1": 11, "2": 99, "3": 30 }),
    { "1": 11 },
  );
});

test("retains the original attempt deadline after a reload", () => {
  const startedAt = 1_000_000;

  assert.equal(secondsRemaining(startedAt, 60, startedAt + 10_000), 50);
  assert.equal(secondsRemaining(startedAt, 60, startedAt + 61_000), 0);
});

test("rejects cached state for a different student or exam", () => {
  const stored = JSON.stringify({
    userId: "7",
    examId: "12",
    startedAt: 1_000_000,
    answers: {},
  });
  const storage = { getItem: () => stored };

  assert.equal(readAttempt(storage, 8, 12).attempt, null);
  assert.equal(readAttempt(storage, 7, 13).attempt, null);
});

test("reports storage and serialization failures instead of silently restoring defaults", () => {
  const inaccessibleStorage = {
    getItem() {
      throw new Error("storage blocked");
    },
  };
  const malformedStorage = { getItem: () => "{" };

  assert.match(readAttempt(inaccessibleStorage, 7, 12).warning, /could not restore/);
  assert.match(readAttempt(malformedStorage, 7, 12).warning, /could not restore/);
});
