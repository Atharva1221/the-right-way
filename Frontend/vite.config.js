import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const environment = loadEnv(mode, process.cwd(), "");
  const backend = environment.VITE_BACKEND_PROXY_TARGET || "http://localhost:8080";

  return {
    plugins: [react()],
    server: {
      proxy: {
        "/api": {
          target: backend,
          changeOrigin: true,
        },
        "/oauth2": {
          target: backend,
          changeOrigin: true,
        },
        "/login/oauth2": {
          target: backend,
          changeOrigin: true,
        },
      },
    },
  };
});
