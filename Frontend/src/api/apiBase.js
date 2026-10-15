export function resolveApiBase(configuredBase, isProduction) {
  const value = configuredBase?.trim();
  if (!value) {
    return isProduction ? null : "";
  }

  let url;
  try {
    url = new URL(value);
  } catch {
    throw new Error("VITE_API_BASE_URL must be an absolute HTTP or HTTPS URL.");
  }
  if (
    !["http:", "https:"].includes(url.protocol)
    || (isProduction && url.protocol !== "https:")
    || !url.hostname
    || url.username
    || url.password
    || (url.pathname !== "" && url.pathname !== "/")
    || url.search
    || url.hash
  ) {
    throw new Error(
      isProduction
        ? "VITE_API_BASE_URL must be an absolute HTTPS origin in production."
        : "VITE_API_BASE_URL must be an absolute HTTP or HTTPS origin.",
    );
  }
  return url.origin;
}
