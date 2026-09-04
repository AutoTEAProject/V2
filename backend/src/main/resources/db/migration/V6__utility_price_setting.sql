CREATE TABLE utility_price_setting (
    utility_type VARCHAR(20) PRIMARY KEY,
    value DOUBLE PRECISION NOT NULL,
    unit VARCHAR(50) NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO utility_price_setting (utility_type, value, unit) VALUES
    ('COOLING', 0.000078, 'USD/kg'),
    ('HOT', 0.0105, 'USD/kWh'),
    ('ELECTRICITY', 0.108, 'USD/kWh'),
    ('MPSG', 0.055, 'USD/kg');
