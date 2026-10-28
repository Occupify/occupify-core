-- =============================================================================
-- Migration: V8__seed_test_contract.sql
-- Description: Seed initial test contract for manual testing and local dev.
-- =============================================================================

INSERT INTO contracts (
    id,
    job_id,
    employer_id,
    employee_id,
    title,
    job_description,
    agreed_rate,
    salary_type,
    started_at,
    finished_at,
    terms_file_key,
    status,
    created_at,
    updated_at
)
VALUES (
    'c1111111-1111-1111-1111-111111111111',
    'b0000000-0000-0000-0000-000000000001',
    '00000000-0000-0000-0000-000000000001',
    '33333333-3333-3333-3333-333333333333',
    'Senior Backend Engineer - Core Payments',
    'Xây dựng kiến trúc backend và các module thanh toán lõi',
    20000000.00,
    'MONTHLY',
    NULL,
    '2026-12-31 00:00:00+00',
    'contracts/c1.pdf',
    'PENDING',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
