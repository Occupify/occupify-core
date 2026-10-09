-- Enable UUID extension if supported
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. users
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    full_name VARCHAR(150),
    avatar_url VARCHAR(500),
    role VARCHAR(50) NOT NULL CHECK (UPPER(role) IN ('FREELANCER', 'CLIENT', 'ADMIN')),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (UPPER(status) IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_users_status ON users(status);
CREATE INDEX IF NOT EXISTS idx_users_role ON users(role);

-- 2. skills
CREATE TABLE IF NOT EXISTS skills (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. projects
CREATE TABLE IF NOT EXISTS projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_price NUMERIC(12, 2),
    period VARCHAR(50) CHECK (period IS NULL OR UPPER(period) IN ('FIXED', 'HOURLY', 'WEEKLY', 'MONTHLY')),
    due_date DATE,
    experience_level VARCHAR(50) CHECK (experience_level IS NULL OR UPPER(experience_level) IN ('ENTRY', 'INTERMEDIATE', 'EXPERT')),
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN' CHECK (UPPER(status) IN ('OPEN', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_projects_employer_id ON projects(employer_id);
CREATE INDEX IF NOT EXISTS idx_projects_status ON projects(status);

-- 4. job
CREATE TABLE IF NOT EXISTS job (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    quantity INT NOT NULL DEFAULT 1,
    salary_type VARCHAR(50) CHECK (salary_type IS NULL OR UPPER(salary_type) IN ('FIXED', 'HOURLY', 'WEEKLY', 'MONTHLY')),
    budget_min NUMERIC(12, 2),
    budget_max NUMERIC(12, 2),
    status VARCHAR(50) NOT NULL DEFAULT 'RECRUITING' CHECK (UPPER(status) IN ('RECRUITING', 'FILLED', 'CLOSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_project_id ON job(project_id);
CREATE INDEX IF NOT EXISTS idx_job_status ON job(status);

-- 5. job_skills (join table for M:N)
CREATE TABLE IF NOT EXISTS job_skills (
    job_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    skill_id UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (job_id, skill_id)
);

CREATE INDEX IF NOT EXISTS idx_job_skills_skill_id ON job_skills(skill_id);

-- 6. bids
CREATE TABLE IF NOT EXISTS bids (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    freelancer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    proposed_salary_type VARCHAR(50) CHECK (proposed_salary_type IS NULL OR UPPER(proposed_salary_type) IN ('FIXED', 'HOURLY', 'WEEKLY', 'MONTHLY')),
    proposed_rate NUMERIC(12, 2),
    commitment_duration VARCHAR(100),
    weekly_commitment_hours VARCHAR(50),
    message TEXT,
    attachment_cv_url VARCHAR(255),
    portfolio_link VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (UPPER(status) IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_bids_job_id ON bids(job_id);
CREATE INDEX IF NOT EXISTS idx_bids_freelancer_id ON bids(freelancer_id);
CREATE INDEX IF NOT EXISTS idx_bids_status ON bids(status);

-- 7. contracts
CREATE TABLE IF NOT EXISTS contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL REFERENCES job(id) ON DELETE CASCADE,
    freelancer_id UUID REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    budget NUMERIC(12, 2),
    salary_type VARCHAR(50) CHECK (salary_type IS NULL OR UPPER(salary_type) IN ('FIXED', 'HOURLY', 'WEEKLY', 'MONTHLY')),
    start_date DATE,
    end_date DATE,
    attachment_url VARCHAR(500),
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (UPPER(status) IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_contracts_job_id ON contracts(job_id);
CREATE INDEX IF NOT EXISTS idx_contracts_freelancer_id ON contracts(freelancer_id);
CREATE INDEX IF NOT EXISTS idx_contracts_status ON contracts(status);

-- 8. offers
CREATE TABLE IF NOT EXISTS offers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    freelancer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    proposed_rate NUMERIC(12, 2),
    offer_message TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (UPPER(status) IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_offers_contract_id ON offers(contract_id);
CREATE INDEX IF NOT EXISTS idx_offers_freelancer_id ON offers(freelancer_id);
CREATE INDEX IF NOT EXISTS idx_offers_status ON offers(status);

-- 9. reviews
CREATE TABLE IF NOT EXISTS reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES contracts(id) ON DELETE CASCADE,
    reviewer_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reviewee_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    comment TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reviews_contract_id ON reviews(contract_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewer_id ON reviews(reviewer_user_id);
CREATE INDEX IF NOT EXISTS idx_reviews_reviewee_id ON reviews(reviewee_user_id);

-- 10. reports
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    target_type VARCHAR(50) NOT NULL CHECK (UPPER(target_type) IN ('USER', 'PROJECT')),
    target_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    target_project_id UUID REFERENCES projects(id) ON DELETE SET NULL,
    reason_category VARCHAR(50) NOT NULL CHECK (UPPER(reason_category) IN ('FRAUD', 'OUTSIDE_PAYMENT', 'FAKE_INFORMATION', 'SPAM_HARASSMENT', 'OTHER')),
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (UPPER(status) IN ('PENDING', 'RESOLVED', 'DISMISSED')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reports_reporter_id ON reports(reporter_user_id);
CREATE INDEX IF NOT EXISTS idx_reports_target_user_id ON reports(target_user_id);
CREATE INDEX IF NOT EXISTS idx_reports_target_project_id ON reports(target_project_id);
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status);

-- 11. report_evidences
CREATE TABLE IF NOT EXISTS report_evidences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    report_id UUID NOT NULL REFERENCES reports(id) ON DELETE CASCADE,
    file_url VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_report_evidences_report_id ON report_evidences(report_id);
