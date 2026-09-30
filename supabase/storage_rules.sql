-- =========================================================================
-- Supabase Storage Configuration & Access Policies for ApplyTrack
-- =========================================================================
-- This script configures the storage bucket and access policies required for
-- ApplyTrack resume and document attachment synchronization.
--
-- Architecture Note:
-- ApplyTrack uses Firebase Authentication (Google / Anonymous sign-in) as its
-- primary identity provider. Attachments are synchronized to Supabase Storage
-- partitioned under unlisted, user-scoped UUID path structures:
--   users/{userId}/{type}/{fileName}
--   (e.g., users/5F8a.../resumes/my_resume.pdf)
--
-- The bucket is created with public read access so client applications
-- (Android Compose & React Web) can directly fetch and preview attachments
-- without requiring an intermediate backend signed-URL proxy server.
--
-- Run this in your Supabase SQL Editor: Dashboard -> SQL Editor -> New Query.
-- =========================================================================

-- 1. Ensure the 'ApplyTrack' storage bucket exists with public read access
INSERT INTO storage.buckets (id, name, public)
VALUES ('ApplyTrack', 'ApplyTrack', true)
ON CONFLICT (id) DO NOTHING;

-- 2. Allow clients to upload attachment files under the 'users/' path partition
CREATE POLICY "Allow client uploads to user directories"
ON storage.objects
FOR INSERT
TO anon, authenticated
WITH CHECK (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[1] = 'users'
);

-- 3. Allow clients to update/overwrite attachments in user directories
CREATE POLICY "Allow client updates to user directories"
ON storage.objects
FOR UPDATE
TO anon, authenticated
USING (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[1] = 'users'
);

-- 4. Allow clients to delete attachments in user directories
CREATE POLICY "Allow client deletes from user directories"
ON storage.objects
FOR DELETE
TO anon, authenticated
USING (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[1] = 'users'
);

-- 5. Public read access policy for viewing and downloading attachments
CREATE POLICY "Allow public read access to user attachments"
ON storage.objects
FOR SELECT
TO public
USING (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[1] = 'users'
);
