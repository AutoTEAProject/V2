ALTER TABLE calculation_run ADD COLUMN stream_snapshot TEXT;

CREATE TABLE project_stream_setting (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES tea_case(id) ON DELETE CASCADE,
    stream_name VARCHAR(200) NOT NULL,
    direction VARCHAR(10) NOT NULL,
    cost DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (case_id, stream_name)
);
