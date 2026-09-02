CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    google_sub VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    picture_url VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    last_login_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE equipment_formula_template (
    id BIGSERIAL PRIMARY KEY,
    equipment_type VARCHAR(20) NOT NULL,
    name VARCHAR(200) NOT NULL,
    k1 DOUBLE PRECISION NOT NULL,
    k2 DOUBLE PRECISION NOT NULL,
    k3 DOUBLE PRECISION NOT NULL,
    is_system_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (equipment_type, name)
);

INSERT INTO equipment_formula_template (equipment_type, name, k1, k2, k3, is_system_default) VALUES
    ('HTX', 'Diphenyl heater', 2.2628, 0.8581, 0.0003, true),
    ('HTX', 'Molten salt heater', 1.1979, 1.4782, -0.0958, true),
    ('HTX', 'Hot water heater', 2.0829, 0.9074, -0.0243, true),
    ('HTX', 'Steam boiler', 6.9617, -1.48, 0.3161, true),
    ('HEX', 'Fixed tube', 4.3247, -0.303, 0.1634, true),
    ('HEX', 'Floating head', 4.8306, -0.8509, 0.3187, true),
    ('HEX', 'U-tube', 4.1884, -0.2503, 0.1974, true),
    ('HEX', 'Bayonet', 4.2768, -0.0495, 0.1431, true),
    ('COMP', 'Centrifugal, axial and reciprocating', 2.2897, 1.3604, -0.1027, true),
    ('COMP', 'Rotary', 5.0355, -1.8002, 0.8253, true);

-- project_equipment_setting rows represent either a type-level default
-- (instance_name = '*') or an override for one named piece of equipment
-- discovered by parsing a project's input.rep/input.xlsx.
CREATE TABLE project_equipment_setting (
    id BIGSERIAL PRIMARY KEY,
    case_id BIGINT NOT NULL REFERENCES tea_case(id) ON DELETE CASCADE,
    equipment_type VARCHAR(20) NOT NULL,
    instance_name VARCHAR(200) NOT NULL DEFAULT '*',
    skip_cost BOOLEAN NOT NULL DEFAULT false,
    default_formula_template_id BIGINT REFERENCES equipment_formula_template(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (case_id, equipment_type, instance_name)
);

CREATE TABLE project_equipment_selected_formula (
    setting_id BIGINT NOT NULL REFERENCES project_equipment_setting(id) ON DELETE CASCADE,
    formula_template_id BIGINT NOT NULL REFERENCES equipment_formula_template(id) ON DELETE CASCADE,
    PRIMARY KEY (setting_id, formula_template_id)
);

CREATE TABLE project_equipment_utility_type (
    setting_id BIGINT NOT NULL REFERENCES project_equipment_setting(id) ON DELETE CASCADE,
    utility_type VARCHAR(20) NOT NULL,
    PRIMARY KEY (setting_id, utility_type)
);

ALTER TABLE calculation_run ADD COLUMN equipment_snapshot TEXT;
