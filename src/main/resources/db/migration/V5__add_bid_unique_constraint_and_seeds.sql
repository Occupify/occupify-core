-- =============================================================================
-- Migration: V5__add_bid_unique_constraint_and_seeds.sql
-- Description: Add unique constraint on (job_id, freelancer_id), performance index,
--              and seed mock bid data for testing and development.
-- =============================================================================

-- 1. Ensure single bid per freelancer per job
CREATE UNIQUE INDEX IF NOT EXISTS uk_bids_job_freelancer ON bids(job_id, freelancer_id);

-- 2. Performance index for querying employee bids by status
CREATE INDEX IF NOT EXISTS idx_bids_freelancer_status ON bids(freelancer_id, status);

-- 3. Seed initial mock applicant proposals
INSERT INTO bids (
    id,
    job_id,
    freelancer_id,
    proposed_salary_type,
    proposed_rate,
    commitment_duration,
    weekly_commitment_hours,
    message,
    attachment_cv_url,
    portfolio_url,
    status,
    created_at,
    updated_at
)
VALUES
    (
        'd0000000-0000-0000-0000-000000000001',
        'b0000000-0000-0000-0000-000000000001',
        '33333333-3333-3333-3333-333333333333',
        'MONTHLY',
        38000000.00,
        'Dài hạn (> 6 tháng)',
        '40h / tuần',
        'Tôi có hơn 5 năm kinh nghiệm trong lĩnh vực UI/UX Design và Fullstack Development. Sẵn sàng tham gia dự án ngay.',
        'https://storage.occupify.com/cv/cv_tranminhkhoa.pdf',
        'https://behance.net/minhkhoa_ux',
        'PENDING',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours'
    ),
    (
        'd0000000-0000-0000-0000-000000000002',
        'b0000000-0000-0000-0000-000000000004',
        '66666666-6666-6666-6666-666666666666',
        'MONTHLY',
        13500000.00,
        '3 tháng',
        'Toàn thời gian (~40h / tuần)',
        'Tôi có 3 năm kinh nghiệm Automation Test với Appium và Postman cho ứng dụng di động.',
        'https://storage.occupify.com/cv/cv_ducanh_qa.pdf',
        'https://github.com/ducanh-qa',
        'PENDING',
        CURRENT_TIMESTAMP - INTERVAL '1 day',
        CURRENT_TIMESTAMP - INTERVAL '1 day'
    )
ON CONFLICT (id) DO NOTHING;
