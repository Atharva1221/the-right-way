import assert from "node:assert/strict";
import test from "node:test";
import { validatePhoneNumber, validateRegistration } from "./registrationValidation.js";

const validFields = {
  name: "Student Example",
  email: "student@example.test",
  phoneNumber: "+1 (415) 555-0134",
  password: "StudentPassword123!",
  confirmPassword: "StudentPassword123!",
};

test("accepts registration fields that satisfy backend limits and formats", () => {
  assert.deepEqual(validateRegistration(validFields), {});
});

test("reports required, malformed, overlong, and mismatched registration fields", () => {
  const errors = validateRegistration({
    name: " ",
    email: "not-an-email",
    phoneNumber: "abc",
    password: "short",
    confirmPassword: "different",
  });

  assert.equal(errors.name, "Name is required.");
  assert.equal(errors.email, "Enter a valid email address.");
  assert.match(errors.phoneNumber, /international phone number/);
  assert.match(errors.password, /8 and 72/);
  assert.equal(errors.confirmPassword, "Passwords do not match.");
});

test("validates incomplete Google profile phone numbers using backend constraints", () => {
  assert.equal(validatePhoneNumber("+44 (20) 1234-5678"), true);
  assert.equal(validatePhoneNumber("+91 abc 123"), false);
  assert.equal(validatePhoneNumber("123456"), false);
});
