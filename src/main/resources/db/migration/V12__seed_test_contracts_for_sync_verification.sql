-- =============================================================================
-- Migration: V12__seed_test_contracts_for_sync_verification.sql
-- Service: occupify-core
-- Description: Seed test contracts for manual verification across 5 microservices:
--   - c2222222-... : IN_PROGRESS (Employer: Dev Mock, Employee: Khoa Nguyen) -> Test Complete (OC-98)
--   - c3333333-... : IN_PROGRESS (Employer: Dev Mock, Employee: Khoa Nguyen) -> Test Cancel by Employer (OC-99)
--   - c4444444-... : IN_PROGRESS (Employer: Hoa Le, Employee: Dev Mock)      -> Test Cancel by Freelancer (OC-99)
--   - c5555555-... : COMPLETED   (Employer: Dev Mock, Employee: Khoa Nguyen) -> Test Create Review (OC-106)
--   - c6666666-... : PENDING     (Employer: Hoa Le, Employee: Dev Mock)      -> Test Freelancer Accept / Reject
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
    freelancer_email,
    role,
    created_at,
    updated_at
)
VALUES
    -- 1. Contract c2 (IN_PROGRESS) -> Ready for testing CONTRACT COMPLETE (OC-98)
    (
        'c2222222-2222-2222-2222-222222222222',
        'b0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'Frontend Developer - Mobile App',
        'Phát triển ứng dụng mobile đa nền tảng bằng React Native',
        15000000.00,
        'MONTHLY',
        CURRENT_TIMESTAMP - INTERVAL '14 days',
        '2026-12-31 00:00:00+00',
        'contracts/c2.pdf',
        'IN_PROGRESS',
        'khoa.nguyen@gmail.com',
        'Frontend Developer',
        CURRENT_TIMESTAMP - INTERVAL '14 days',
        CURRENT_TIMESTAMP
    ),

    -- 2. Contract c3 (IN_PROGRESS) -> Ready for testing CONTRACT CANCEL by Employer (OC-99)
    (
        'c3333333-3333-3333-3333-333333333333',
        'b0000000-0000-0000-0000-000000000003',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'UI/UX Designer - Design System',
        'Thiết kế Design System và Prototype trải nghiệm người dùng di động',
        12000000.00,
        'MONTHLY',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        '2026-12-31 00:00:00+00',
        'contracts/c3.pdf',
        'IN_PROGRESS',
        'khoa.nguyen@gmail.com',
        'UI/UX Designer',
        CURRENT_TIMESTAMP - INTERVAL '7 days',
        CURRENT_TIMESTAMP
    ),

    -- 3. Contract c4 (IN_PROGRESS) -> Ready for testing CONTRACT CANCEL by Freelancer (Dev Mock) (OC-99)
    (
        'c4444444-4444-4444-4444-444444444444',
        'b0000000-0000-0000-0000-000000000005',
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000001',
        'Frontend Engineer - React Component Library',
        'Xây dựng các component React tái sử dụng kèm Storybook',
        20000000.00,
        'MONTHLY',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        '2026-12-31 00:00:00+00',
        'contracts/c4.pdf',
        'IN_PROGRESS',
        'dev-employer@occupify.local',
        'Frontend Engineer',
        CURRENT_TIMESTAMP - INTERVAL '5 days',
        CURRENT_TIMESTAMP
    ),

    -- 4. Contract c5 (COMPLETED) -> Ready for testing CREATE REVIEW (OC-106)
    (
        'c5555555-5555-5555-5555-555555555555',
        'b0000000-0000-0000-0000-000000000004',
        '00000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'QA / Tester Lead - Automation Suite',
        'Viết kịch bản kiểm thử tự động trên mobile và API',
        10000000.00,
        'MONTHLY',
        CURRENT_TIMESTAMP - INTERVAL '30 days',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        'contracts/c5.pdf',
        'COMPLETED',
        'khoa.nguyen@gmail.com',
        'QA / Tester Lead',
        CURRENT_TIMESTAMP - INTERVAL '30 days',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    ),

    -- 5. Contract c6 (PENDING) -> Ready for testing FREELANCER ACCEPT / REJECT (Dev Mock as Freelancer)
    (
        'c6666666-6666-6666-6666-666666666666',
        'b0000000-0000-0000-0000-000000000006',
        '11111111-1111-1111-1111-111111111111',
        '00000000-0000-0000-0000-000000000001',
        'UI/UX Lead - SaaS Tokens & Guidelines',
        'Quy hoạch kiến trúc Design Tokens và Guidelines cho SaaS',
        25000000.00,
        'MONTHLY',
        NULL,
        '2026-12-31 00:00:00+00',
        'contracts/c6.pdf',
        'PENDING',
        'dev-employer@occupify.local',
        'UI/UX Lead',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;
