import { resolveApiBase } from "./apiBase.js";

const configuredApiBase = import.meta.env.VITE_API_BASE_URL;
let apiBase = null;
let apiBaseConfigurationError = null;

try {
  apiBase = resolveApiBase(configuredApiBase, import.meta.env.PROD);
} catch (error) {
  apiBaseConfigurationError = error.message;
}

let accessToken = null;
let refreshInProgress = null;

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function assertApiBaseConfigured() {
  if (apiBaseConfigurationError) {
    throw new ApiError(apiBaseConfigurationError);
  }
  if (apiBase === null) {
    throw new ApiError(
      "The backend API URL is not configured. Set VITE_API_BASE_URL for this production build.",
    );
  }
}

export function setAccessToken(token) {
  accessToken = token || null;
}

export function clearAccessToken() {
  accessToken = null;
}

async function responseError(response) {
  let message = `Request failed (${response.status})`;
  try {
    const body = await response.json();
    if (typeof body?.error === "string" && body.error.trim()) {
      message = body.error;
    }
  } catch {
    // Keep the status-based message for empty and non-JSON error responses.
  }
  return new ApiError(message, response.status);
}

async function ensureCsrfToken() {
  assertApiBaseConfigured();
  const response = await fetch(`${apiBase}/api/auth/csrf`, {
    credentials: "include",
  });
  if (!response.ok) {
    throw await responseError(response);
  }

  const data = await response.json();
  if (typeof data?.token !== "string" || !data.token) {
    throw new ApiError("The security token could not be initialized.");
  }
  return data.token;
}

async function fetchJson(path, options = {}, { csrfToken, useAccessToken = true } = {}) {
  assertApiBaseConfigured();
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  if (csrfToken) {
    headers.set("X-XSRF-TOKEN", csrfToken);
  }
  if (useAccessToken && accessToken) {
    headers.set("Authorization", `Bearer ${accessToken}`);
  }

  let response;
  try {
    response = await fetch(`${apiBase}${path}`, {
      ...options,
      headers,
      credentials: "include",
    });
  } catch {
    throw new ApiError(
      "Unable to reach the server. Check your connection and try again.",
    );
  }

  if (!response.ok) {
    throw await responseError(response);
  }
  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("Content-Type") || "";
  if (!contentType.includes("application/json")) {
    throw new ApiError("The server returned an unexpected response.");
  }
  return response.json();
}

async function refreshAccessToken() {
  if (!refreshInProgress) {
    refreshInProgress = (async () => {
      const csrfToken = await ensureCsrfToken();
      const response = await fetch(`${apiBase}/api/auth/refresh`, {
        method: "POST",
        headers: { "X-XSRF-TOKEN": csrfToken },
        credentials: "include",
      });

      if (!response.ok) {
        throw await responseError(response);
      }
      const data = await response.json();
      if (typeof data.accessToken !== "string" || !data.accessToken) {
        throw new ApiError("The server did not return an access token.");
      }
      accessToken = data.accessToken;
      return data;
    })().finally(() => {
      refreshInProgress = null;
    });
  }
  return refreshInProgress;
}

export async function restoreSession() {
  try {
    await refreshAccessToken();
    return await fetchJson("/api/auth/me");
  } catch (error) {
    clearAccessToken();
    if (error instanceof ApiError && error.status === 401) {
      return null;
    }
    throw error;
  }
}

export async function apiRequest(path, options = {}) {
  const method = (options.method || "GET").toUpperCase();
  const isUnsafeMethod = !["GET", "HEAD", "OPTIONS"].includes(method);
  const csrfToken = isUnsafeMethod ? await ensureCsrfToken() : undefined;

  try {
    return await fetchJson(path, options, { csrfToken });
  } catch (error) {
    const isAuthEndpoint = path.startsWith("/api/auth/");
    if (!(error instanceof ApiError) || error.status !== 401 || !accessToken || isAuthEndpoint) {
      throw error;
    }

    try {
      await refreshAccessToken();
      const retryCsrfToken = isUnsafeMethod ? await ensureCsrfToken() : undefined;
      return await fetchJson(path, options, { csrfToken: retryCsrfToken });
    } catch (refreshError) {
      clearAccessToken();
      if (refreshError instanceof ApiError && refreshError.status === 401) {
        window.dispatchEvent(new Event("exam-platform:session-expired"));
      }
      throw refreshError;
    }
  }
}

export async function login(email, password) {
  const csrfToken = await ensureCsrfToken();
  const data = await fetchJson(
    "/api/auth/login",
    {
      method: "POST",
      body: JSON.stringify({ email, password }),
    },
    { csrfToken, useAccessToken: false },
  );

  if (typeof data.accessToken !== "string" || !data.accessToken) {
    throw new ApiError("The server did not return an access token.");
  }
  accessToken = data.accessToken;

  try {
    return await fetchJson("/api/auth/me");
  } catch (error) {
    clearAccessToken();
    throw error;
  }
}

export async function logout() {
  const csrfToken = await ensureCsrfToken();
  try {
    await fetchJson(
      "/api/auth/logout",
      { method: "POST" },
      { csrfToken, useAccessToken: false },
    );
  } finally {
    clearAccessToken();
  }
}

export function getApiBaseUrl() {
  return apiBase;
}

export function getGoogleAuthorizationUrl() {
  if (apiBaseConfigurationError || apiBase === null) {
    return null;
  }
  return `${apiBase}/oauth2/authorization/google`;
}
