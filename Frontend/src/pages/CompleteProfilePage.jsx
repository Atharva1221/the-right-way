import { useState } from "react";
import { Navigate } from "react-router-dom";
import { ApiError } from "../api/client";
import { useAuth } from "../auth/AuthProvider";
import { validatePhoneNumber } from "../auth/registrationValidation";
import { Alert, Brand, Button, TextField } from "../components/ui";

export function CompleteProfilePage() {
  const { user, updatePhoneNumber, signOut } = useAuth();
  const [phoneNumber, setPhoneNumber] = useState("");
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!user || user.role !== "STUDENT") {
    return <Navigate to="/login" replace />;
  }
  if (user.profileComplete) {
    return <Navigate to="/student/dashboard" replace />;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const normalizedPhone = phoneNumber.trim();
    if (!validatePhoneNumber(normalizedPhone)) {
      setError("Enter a valid international phone number with 7 to 15 digits.");
      return;
    }
    setIsSubmitting(true);
    setError("");
    try {
      await updatePhoneNumber(normalizedPhone);
    } catch (updateError) {
      setError(updateError instanceof ApiError && updateError.status === 400
        ? updateError.message
        : updateError.message || "Your profile could not be updated.");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSignOut() {
    await signOut();
  }

  return (
    <main className="auth-page">
      <section className="auth-card auth-card--message" aria-labelledby="complete-profile-heading">
        <Brand />
        <p className="eyebrow">One last step</p>
        <h1 id="complete-profile-heading">Complete your student profile</h1>
        <p className="login-panel__description">
          Add a phone number to finish setting up your account.
        </p>
        {error && <Alert>{error}</Alert>}
        <form className="login-form" onSubmit={handleSubmit}>
          <TextField
            autoComplete="tel"
            hint="Use an international format, for example +1 555 123 4567."
            id="profile-phone"
            label="Phone number"
            maxLength={25}
            onChange={(event) => setPhoneNumber(event.target.value)}
            required
            type="tel"
            value={phoneNumber}
          />
          <Button disabled={isSubmitting} type="submit">
            {isSubmitting ? "Saving…" : "Save and continue"}
          </Button>
        </form>
        <Button onClick={() => void handleSignOut()} variant="quiet">
          Sign out
        </Button>
      </section>
    </main>
  );
}
