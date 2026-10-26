-- =============================================================================
-- Migration Seed: V3__seed_project_data.sql
-- Description: Seed initial mock data for users, skills, projects, jobs, and job_skills
-- Derived from occupify-web mock data (/src/features/mock-data.ts)
-- =============================================================================

-- 1. SEED USERS
INSERT INTO users (id, email, full_name, avatar_url, role, status, created_at, updated_at)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'dev-employer@occupify.local', 'Tôi (Chủ dự án)', 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('11111111-1111-1111-1111-111111111111', 'hoa.le@smartlog.vn', 'Lê Thị Hoa', 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('22222222-2222-2222-2222-222222222222', 'dung.pham@insightmetrics.vn', 'Phạm Văn Dũng', 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('33333333-3333-3333-3333-333333333333', 'khoa.nguyen@gmail.com', 'Nguyễn Minh Khoa', 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('44444444-4444-4444-4444-444444444444', 'hung.le@gmail.com', 'Lê Văn Hùng', 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('55555555-5555-5555-5555-555555555555', 'thu.nguyen@gmail.com', 'Nguyễn Thị Thu', 'https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('66666666-6666-6666-6666-666666666666', 'duc.anh@gmail.com', 'Phạm Đức Anh', 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?w=150', 'USER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- 2. SEED SKILLS
INSERT INTO skills (id, name, created_at, updated_at)
VALUES
    ('c0000000-0000-0000-0000-000000000001', 'Node.js', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000002', 'NestJS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000003', 'PostgreSQL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000004', 'Redis', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000005', 'React Native', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000006', 'TypeScript', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000007', 'Tailwind CSS', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000008', 'Figma', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000009', 'Design System', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000010', 'User Testing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000011', 'Appium', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000012', 'Postman', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000013', 'Automation Testing', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000014', 'React', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000015', 'Storybook', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000016', 'RESTful API', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000017', 'Python', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000018', 'SQL', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000019', 'PowerBI', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('c0000000-0000-0000-0000-000000000020', 'Docker', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- 3. SEED PROJECTS
INSERT INTO projects (id, employer_id, title, description, start_price, period, due_date, experience_level, status, created_at, updated_at, closed_at)
VALUES
    (
        'a0000000-0000-0000-0000-000000000001',
        '00000000-0000-0000-0000-000000000001',
        'Redesign Mobile App',
        'Tái thiết kế toàn bộ trải nghiệm người dùng, luồng thanh toán và giao diện ứng dụng di động cho hơn 200.000 người dùng hàng ngày.',
        25000000.00,
        'MONTHLY',
        '2026-12-31',
        'EXPERT',
        'OPEN',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        'a0000000-0000-0000-0000-000000000002',
        '00000000-0000-0000-0000-000000000001',
        'Xây dựng Design System SaaS',
        'Quy chuẩn hóa toàn bộ thư viện component, token màu sắc, typography và micro-interactions cho hệ sinh thái sản phẩm SaaS B2B.',
        35000000.00,
        'MONTHLY',
        '2027-03-31',
        'INTERMEDIATE',
        'OPEN',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        'a0000000-0000-0000-0000-000000000003',
        '11111111-1111-1111-1111-111111111111',
        'Xây dựng API REST cho hệ thống ERP',
        'Tái cấu trúc và xây dựng bộ API RESTful toàn diện kết nối các phân hệ nhân sự, chuỗi cung ứng, kho vận và kế toán doanh nghiệp cho đối tác quốc tế.',
        40000000.00,
        'FIXED',
        '2027-01-15',
        'EXPERT',
        'OPEN',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        'a0000000-0000-0000-0000-000000000004',
        '22222222-2222-2222-2222-222222222222',
        'Phân tích dữ liệu người dùng Q4',
        'Nghiên cứu hành vi khách hàng, xây dựng dashboard phân tích chỉ số chuyển đổi, phễu mua hàng và dự báo churn rate cho sàn thương mại điện tử hàng đầu.',
        18000000.00,
        'MONTHLY',
        '2026-12-25',
        'INTERMEDIATE',
        'OPEN',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL
    ),
    (
        'a0000000-0000-0000-0000-000000000005',
        '11111111-1111-1111-1111-111111111111',
        'Thiết kế landing page chiến dịch Marketing',
        'Xây dựng landing page tối ưu chuyển đổi và animation mượt mà phục vụ chiến dịch mở bán sản phẩm Q3/2026.',
        12000000.00,
        'FIXED',
        '2026-08-20',
        'ENTRY',
        'CLOSED',
        CURRENT_TIMESTAMP - INTERVAL '60 days',
        CURRENT_TIMESTAMP - INTERVAL '10 days',
        CURRENT_TIMESTAMP - INTERVAL '10 days'
    )
ON CONFLICT (id) DO NOTHING;

-- 4. SEED JOBS
INSERT INTO job (id, project_id, title, description, quantity, salary_type, budget_min, budget_max, status, created_at, updated_at)
VALUES
    -- Project 1 Jobs
    (
        'b0000000-0000-0000-0000-000000000001',
        'a0000000-0000-0000-0000-000000000001',
        'Backend Developer',
        'Xây dựng kiến trúc backend và các module thanh toán lõi',
        1,
        'MONTHLY',
        18000000.00,
        25000000.00,
        'RECRUITING',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000002',
        'a0000000-0000-0000-0000-000000000001',
        'Frontend Developer',
        'Phát triển ứng dụng mobile đa nền tảng bằng React Native',
        1,
        'MONTHLY',
        15000000.00,
        22000000.00,
        'FILLED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000003',
        'a0000000-0000-0000-0000-000000000001',
        'UI/UX Designer',
        'Thiết kế Design System và Prototype trải nghiệm người dùng di động',
        1,
        'MONTHLY',
        12000000.00,
        18000000.00,
        'FILLED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000004',
        'a0000000-0000-0000-0000-000000000001',
        'QA / Tester Lead',
        'Viết kịch bản kiểm thử tự động trên mobile và API',
        1,
        'MONTHLY',
        10000000.00,
        15000000.00,
        'RECRUITING',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Project 2 Jobs
    (
        'b0000000-0000-0000-0000-000000000005',
        'a0000000-0000-0000-0000-000000000002',
        'Frontend Engineer',
        'Xây dựng các component React tái sử dụng kèm Storybook',
        1,
        'MONTHLY',
        20000000.00,
        28000000.00,
        'RECRUITING',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),
    (
        'b0000000-0000-0000-0000-000000000006',
        'a0000000-0000-0000-0000-000000000002',
        'UI/UX Lead',
        'Quy hoạch kiến trúc Design Tokens và Guidelines cho SaaS',
        1,
        'MONTHLY',
        25000000.00,
        35000000.00,
        'FILLED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Project 3 Jobs
    (
        'b0000000-0000-0000-0000-000000000007',
        'a0000000-0000-0000-0000-000000000003',
        'Backend Developer',
        'Thiết kế và triển khai các API RESTful tích hợp ERP',
        1,
        'FIXED',
        35000000.00,
        40000000.00,
        'FILLED',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    ),

    -- Project 4 Jobs
    (
        'b0000000-0000-0000-0000-000000000008',
        'a0000000-0000-0000-0000-000000000004',
        'Data & BI Analyst',
        'Phân tích phễu chuyển đổi và thiết lập dashboard PowerBI',
        1,
        'MONTHLY',
        16000000.00,
        22000000.00,
        'RECRUITING',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
ON CONFLICT (id) DO NOTHING;

-- 5. SEED JOB_SKILLS MAPPINGS
INSERT INTO job_skills (job_id, skill_id)
SELECT j.id, s.id
FROM (VALUES
    ('b0000000-0000-0000-0000-000000000001'::uuid, 'Node.js'),
    ('b0000000-0000-0000-0000-000000000001'::uuid, 'NestJS'),
    ('b0000000-0000-0000-0000-000000000001'::uuid, 'PostgreSQL'),
    ('b0000000-0000-0000-0000-000000000001'::uuid, 'Redis'),
    ('b0000000-0000-0000-0000-000000000002'::uuid, 'React Native'),
    ('b0000000-0000-0000-0000-000000000002'::uuid, 'TypeScript'),
    ('b0000000-0000-0000-0000-000000000002'::uuid, 'Tailwind CSS'),
    ('b0000000-0000-0000-0000-000000000003'::uuid, 'Figma'),
    ('b0000000-0000-0000-0000-000000000003'::uuid, 'Design System'),
    ('b0000000-0000-0000-0000-000000000003'::uuid, 'User Testing'),
    ('b0000000-0000-0000-0000-000000000004'::uuid, 'Appium'),
    ('b0000000-0000-0000-0000-000000000004'::uuid, 'Postman'),
    ('b0000000-0000-0000-0000-000000000004'::uuid, 'Automation Testing'),
    ('b0000000-0000-0000-0000-000000000005'::uuid, 'React'),
    ('b0000000-0000-0000-0000-000000000005'::uuid, 'Storybook'),
    ('b0000000-0000-0000-0000-000000000005'::uuid, 'Tailwind CSS'),
    ('b0000000-0000-0000-0000-000000000006'::uuid, 'Figma'),
    ('b0000000-0000-0000-0000-000000000006'::uuid, 'Design System'),
    ('b0000000-0000-0000-0000-000000000007'::uuid, 'PostgreSQL'),
    ('b0000000-0000-0000-0000-000000000007'::uuid, 'RESTful API'),
    ('b0000000-0000-0000-0000-000000000008'::uuid, 'Python'),
    ('b0000000-0000-0000-0000-000000000008'::uuid, 'SQL'),
    ('b0000000-0000-0000-0000-000000000008'::uuid, 'PowerBI')
) AS mapping(job_id, skill_name)
JOIN job j ON j.id = mapping.job_id
JOIN skills s ON s.name = mapping.skill_name
ON CONFLICT (job_id, skill_id) DO NOTHING;

-- 6. SEED TEST CONTRACT (Pending acceptance)
INSERT INTO contracts (id, job_id, freelancer_id, title, description, budget, salary_type, start_date, end_date, attachment_url, status, created_at, updated_at)
VALUES (
    'c1111111-1111-1111-1111-111111111111',
    'b0000000-0000-0000-0000-000000000001',
    '33333333-3333-3333-3333-333333333333',
    'Senior Backend Engineer - Core Payments',
    'Xây dựng kiến trúc backend và các module thanh toán lõi',
    20000000.00,
    'MONTHLY',
    NULL,
    '2026-12-31',
    'https://storage.occupify.work/contracts/c1.pdf',
    'PENDING',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;
