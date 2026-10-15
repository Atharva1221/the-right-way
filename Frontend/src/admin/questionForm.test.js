import assert from "node:assert/strict";
import test from "node:test";
import { updateQuestionOptions } from "./questionForm.js";

test("editing option text leaves other option text unchanged", () => {
  const options = [
    { optionText: "First", correct: true, optionOrder: 1 },
    { optionText: "Second", correct: false, optionOrder: 2 },
  ];

  const updated = updateQuestionOptions(options, 1, "optionText", "Revised second");

  assert.deepEqual(updated.map((option) => option.optionText), ["First", "Revised second"]);
  assert.deepEqual(options.map((option) => option.optionText), ["First", "Second"]);
});

test("choosing the correct option clears the previous correct flag", () => {
  const options = [
    { optionText: "First", correct: true, optionOrder: 1 },
    { optionText: "Second", correct: false, optionOrder: 2 },
  ];

  const updated = updateQuestionOptions(options, 1, "correct", true);

  assert.deepEqual(updated.map((option) => option.correct), [false, true]);
});
