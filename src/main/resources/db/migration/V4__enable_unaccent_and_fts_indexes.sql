-- =========================================================================
-- V4: Enable PostgreSQL unaccent extension and create FTS & performance indexes
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS "unaccent";

-- Create immutable wrapper for unaccent to allow indexing in PostgreSQL
CREATE OR REPLACE FUNCTION f_unaccent(text)
  RETURNS text AS $$
    SELECT public.unaccent('public.unaccent', $1)
  $$ LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT;

-- GIN index on unaccented project title and description
CREATE INDEX IF NOT EXISTS idx_projects_fts 
  ON projects USING gin(to_tsvector('simple', f_unaccent(coalesce(title, '') || ' ' || coalesce(description, ''))));

-- B-Tree index on job project and status for active recruiting job lookup
CREATE INDEX IF NOT EXISTS idx_job_project_status 
  ON job(project_id, status);

-- GIN index on unaccented job title and description
CREATE INDEX IF NOT EXISTS idx_job_fts 
  ON job USING gin(to_tsvector('simple', f_unaccent(coalesce(title, '') || ' ' || coalesce(description, ''))));

-- GIN index on unaccented skills name
CREATE INDEX IF NOT EXISTS idx_skills_name_fts 
  ON skills USING gin(to_tsvector('simple', f_unaccent(name)));
