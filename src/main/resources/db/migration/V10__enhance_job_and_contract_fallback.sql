-- 1. Enhance job table with role, budget, attachment and dates
ALTER TABLE job ADD COLUMN IF NOT EXISTS role VARCHAR(100);
ALTER TABLE job ADD COLUMN IF NOT EXISTS budget DECIMAL(12, 2);
ALTER TABLE job ADD COLUMN IF NOT EXISTS terms_file_key VARCHAR(500);
ALTER TABLE job ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;
ALTER TABLE job ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;
ALTER TABLE job ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 2. Enhance contracts table with freelancer email, message, and soft delete
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS freelancer_email VARCHAR(255);
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS message TEXT;
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS role VARCHAR(100);
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;

-- 3. Relax NOT NULL constraints for fallback fields
ALTER TABLE contracts ALTER COLUMN agreed_rate DROP NOT NULL;
ALTER TABLE contracts ALTER COLUMN salary_type DROP NOT NULL;

-- 4. Indexes
CREATE INDEX IF NOT EXISTS idx_job_project_id ON job(project_id);
CREATE INDEX IF NOT EXISTS idx_job_role ON job(role);
CREATE INDEX IF NOT EXISTS idx_contracts_job_id ON contracts(job_id);
CREATE INDEX IF NOT EXISTS idx_contracts_freelancer_email ON contracts(freelancer_email);
