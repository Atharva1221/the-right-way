import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ApiError, apiRequest } from "../api/client";
import { validateRegistration } from "../auth/registrationValidation";
import { Alert, Brand, Button, TextField } from "../components/ui";

export function RegistrationPage() {
  const navigate = useNavigate();
  const [fields, setFields] = useState({
    name: "",
    email: "",
    phoneNumber: "",
    password: "",
    confirmPassword: "",
  });
  const [fieldErrors, setFieldErrors] = useState({});
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function updateField(event) {
    const { name, value } = event.target;
    setFields((current) => ({ ...current, [name]: value }));
    setFieldErrors((current) => ({ ...current, [name]: "" }));
    setError("");
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const errors = validateRegistration(fields);
    setFieldErrors(errors);
    setError("");
    if (Object.keys(errors).length > 0) return;

    setIsSubmitting(true);
    try {
      const response = await apiRequest("/api/auth/register", {
        method: "POST",
        body: JSON.stringify({
          name: fields.name.trim(),
          email: fields.email.trim(),
          phoneNumber: fields.phoneNumber.trim(),
          password: fields.password,
        }),
      });
      if (
        !response
        || response.role !== "STUDENT"
        || response.accessToken
        || !response.id
      ) {
        throw new Error("The server returned an unexpected registration response.");
      }
      navigate("/login", {
        replace: true,
        state: { registrationSuccess: true },
      });
    } catch (registrationError) {
      if (
        registrationError instanceof ApiError
        && registrationError.message.toLowerCase().includes("already registered")
      ) {
        setError("An account with this email already exists. Sign in instead.");
      } else if (registrationError instanceof ApiError && registrationError.status === 400) {
        setError(registrationError.message);
      } else if (registrationError instanceof ApiError && registrationError.status === 409) {
        setError("An account with this email already exists. Sign in instead.");
      } else {
        setError(registrationError.message || "Your account could not be created.");
      }
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="register-heading">
        <Brand />
        <p className="eyebrow">Start learning</p>
        <h1 id="register-heading">Create your student account</h1>
        <p className="login-panel__description">
          Join The Right Way to practice and track your progress.
        </p>
        {error && <Alert>{error}</Alert>}
        <form className="login-form registration-form" onSubmit={handleSubmit} noValidate>
          <TextField
            autoComplete="name"
            error={fieldErrors.name}
            id="register-name"
            label="Full name"
            maxLength={100}
            name="name"
            onChange={updateField}
            required
            value={fields.name}
          />
          <TextField
            autoComplete="email"
            error={fieldErrors.email}
            id="register-email"
            label="Email address"
            maxLength={150}
            name="email"
            onChange={updateField}
            required
            type="email"
            value={fields.email}
          />
          <TextField
            autoComplete="tel"
            error={fieldErrors.phoneNumber}
            hint="Use an international format, for example +1 555 123 4567."
            id="register-phone"
            label="Phone number"
            maxLength={25}
            name="phoneNumber"
            onChange={updateField}
            required
            type="tel"
            value={fields.phoneNumber}
          />
          <TextField
            autoComplete="new-password"
            error={fieldErrors.password}
            hint="Use 8 to 72 characters."
            id="register-password"
            label="Password"
            maxLength={72}
            minLength={8}
            name="password"
            onChange={updateField}
            required
            type="password"
            value={fields.password}
          />
          <TextField
            autoComplete="new-password"
            error={fieldErrors.confirmPassword}
            id="register-confirm-password"
            label="Confirm password"
            name="confirmPassword"
            onChange={updateField}
            required
            type="password"
            value={fields.confirmPassword}
          />
          <Button className="login-submit" disabled={isSubmitting} type="submit">
            {isSubmitting ? "Creating account…" : "Create account"}
          </Button>
        </form>
        <p className="auth-switch">
          Already have an account? <Link to="/login">Sign in</Link>
        </p>
      </section>
    </main>
  );
}
