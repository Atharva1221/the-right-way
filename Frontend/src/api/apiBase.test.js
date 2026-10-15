import assert from "node:assert/strict";
import test from "node:test";
import { resolveApiBase } from "./apiBase.js";

test("local development may use the Vite same-origin proxy", () => {
  assert.equal(resolveApiBase("", false), "");
});

test("production requires an explicit API URL", () => {
  assert.equal(resolveApiBase("", true), null);
});

test("configured absolute API URLs are normalized", () => {
  assert.equal(
    resolveApiBase("https://api.example.test/", true),
    "https://api.example.test",
  );
});

test("rejects unsafe or malformed API URL values", () => {
  assert.throws(() => resolveApiBase("/api", true), /absolute/);
  assert.throws(
    () => resolveApiBase("javascript:alert(1)", true),
    /absolute/,
  );
  assert.throws(
    () => resolveApiBase("https://user:password@api.example.test", true),
    /absolute HTTPS origin/,
  );
  assert.throws(
    () => resolveApiBase("https://api.example.test/base", true),
    /absolute HTTPS origin/,
  );
  assert.throws(
    () => resolveApiBase("http://localhost:8080", true),
    /absolute HTTPS origin/,
  );
});
