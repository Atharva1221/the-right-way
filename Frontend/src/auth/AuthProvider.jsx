import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import * as api from "../api/client";

const AuthContext = createContext(null);

function userFromResponse(response) {
  if (!response || !["ADMIN", "STUDENT"].includes(response.role)) {
    throw new Error("The server returned an unsupported account.");
  }
  return {
    id: response.id,
    name: response.name,
    email: response.email,
    role: response.role,
    phoneNumber: response.phoneNumber ?? null,
    profileComplete: Boolean(response.profileComplete),
  };
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [isLoading, setIsLoading] = useState(true);
  const [sessionError, setSessionError] = useState("");

  const restore = useCallback(async () => {
    setIsLoading(true);
    setSessionError("");
    try {
      const response = await api.restoreSession();
      setUser(response ? userFromResponse(response) : null);
    } catch (error) {
      setUser(null);
      setSessionError(error.message);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    void restore();
  }, [restore]);

  useEffect(() => {
    function handleExpiredSession() {
      setUser(null);
      setSessionError("Your session has expired. Please sign in again.");
    }
    window.addEventListener("exam-platform:session-expired", handleExpiredSession);
    return () => {
      window.removeEventListener("exam-platform:session-expired", handleExpiredSession);
    };
  }, []);

  const signIn = useCallback(async (email, password) => {
    const response = await api.login(email, password);
    const signedInUser = userFromResponse(response);
    setUser(signedInUser);
    setSessionError("");
    return signedInUser;
  }, []);

  const updatePhoneNumber = useCallback(async (phoneNumber) => {
    const response = await api.apiRequest("/api/auth/me/phone-number", {
      method: "PUT",
      body: JSON.stringify({ phoneNumber }),
    });
    const updatedUser = userFromResponse(response);
    setUser(updatedUser);
    return updatedUser;
  }, []);

  const signOut = useCallback(async () => {
    try {
      await api.logout();
      setSessionError("");
    } catch (error) {
      setSessionError(error.message);
    } finally {
      api.clearAccessToken();
      setUser(null);
    }
  }, []);

  const value = useMemo(
    () => ({
      user,
      isLoading,
      sessionError,
      signIn,
      updatePhoneNumber,
      signOut,
      retrySession: restore,
      dismissSessionError: () => setSessionError(""),
    }),
    [user, isLoading, sessionError, signIn, updatePhoneNumber, signOut, restore],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within AuthProvider.");
  }
  return context;
}
