CREATE TABLE tea_case (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE calculation_run (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES tea_case(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL,
    input_xlsx_name VARCHAR(255),
    input_rep_name VARCHAR(255),
    result_path VARCHAR(500),
    error_message TEXT,
    logs TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_calculation_run_case_id ON calculation_run(case_id);
