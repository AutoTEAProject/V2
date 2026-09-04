import { defineRailway, project, service, postgres, github } from "railway/iac";

export default defineRailway(() => {
  const db = postgres("Postgres");

  const pythonEngine = service("python-engine", {
    source: github("AutoTEAProject/V2", { branch: "main", rootDirectory: "python-engine" }),
    build: { dockerfilePath: "Dockerfile" },
    healthcheckPath: "/health",
    env: {
      PORT: "8000",
      CALC_TIMEOUT_SECONDS: "300",
    },
  });

  const backend = service("backend", {
    source: github("AutoTEAProject/V2", { branch: "main", rootDirectory: "backend" }),
    build: { dockerfilePath: "Dockerfile" },
    healthcheckPath: "/actuator/health",
    env: {
      DB_HOST: db.env.PGHOST,
      DB_PORT: db.env.PGPORT,
      DB_NAME: db.env.PGDATABASE,
      DB_USERNAME: db.env.PGUSER,
      DB_PASSWORD: db.env.PGPASSWORD,
      // python-engine은 PORT를 8000으로 고정해뒀고, Railway private networking DNS는
      // "<서비스 이름>.railway.internal" 규칙을 따른다.
      PYTHON_ENGINE_URL: "http://python-engine.railway.internal:8000",
      PYTHON_ENGINE_TIMEOUT: "300",
    },
  });

  const frontend = service("frontend", {
    source: github("AutoTEAProject/V2", { branch: "main", rootDirectory: "frontend" }),
    build: { dockerfilePath: "Dockerfile" },
  });

  return project("autotea-v2", {
    resources: [db, pythonEngine, backend, frontend],
  });
});
