-- 배포 환경에서는 backend와 python-engine이 디스크를 공유하지 않으므로,
-- run에 필요한 파일(업로드된 input, 계산 결과)을 파일시스템이 아니라 DB에 저장한다.
ALTER TABLE calculation_run
    ADD COLUMN input_xlsx_data BYTEA,
    ADD COLUMN input_rep_data BYTEA,
    ADD COLUMN result_data BYTEA,
    ADD COLUMN cost_result TEXT;

ALTER TABLE calculation_run DROP COLUMN result_path;
