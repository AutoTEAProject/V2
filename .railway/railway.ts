import { defineRailway, github, postgres, preserve, project, service, volume } from "railway/iac";

export default defineRailway(() => {
  const Postgres = postgres("Postgres", { region: "sfo" });
  const postgresVolume = volume("postgres-volume", { alerts: { usage: { "100": {}, "80": {}, "95": {} } }, allowOnlineResize: true, region: "sfo", sizeMB: 5000 });
  const backend = service("backend", {
    source: github("AutoTEAProject/V2", { rootDirectory: "backend" }),
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    healthcheck: "/actuator/health",
    replicas: { "sfo": 1 },
    env: { CORS_ALLOWED_ORIGINS: preserve(), DB_HOST: preserve(), DB_NAME: preserve(), DB_PASSWORD: preserve(), DB_PORT: preserve(), DB_USERNAME: preserve(), GOOGLE_CLIENT_ID: preserve(), JWT_SECRET: preserve(), PYTHON_ENGINE_TIMEOUT: preserve(), PYTHON_ENGINE_URL: preserve() },
  });
  const pythonEngine = service("python-engine", {
    source: github("AutoTEAProject/V2", { rootDirectory: "python-engine" }),
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    healthcheck: "/health",
    replicas: { "sfo": 1 },
    env: { CALC_TIMEOUT_SECONDS: preserve(), PORT: preserve() },
  });
  const frontend = service("frontend", {
    source: github("AutoTEAProject/V2", { rootDirectory: "frontend" }),
    build: { buildEnvironment: "V3", builder: "DOCKERFILE", dockerfilePath: "Dockerfile" },
    replicas: { "sfo": 1 },
    env: { VITE_API_BASE_URL: preserve(), VITE_GOOGLE_CLIENT_ID: preserve() },
  });

  return project("autotea-v2", {
    resources: [backend, pythonEngine, frontend, Postgres, postgresVolume],
  });
});
