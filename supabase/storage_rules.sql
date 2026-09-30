-- =========================================================================
-- Supabase Storage Row Level Security (RLS) Policy for ApplyTrack
-- =========================================================================
-- This script configures the storage bucket and policies required for ApplyTrack
-- resume and document attachment synchronization.
-- Run this in your Supabase SQL Editor: Dashboard -> SQL Editor -> New Query.
-- =========================================================================

-- 1. Ensure the 'ApplyTrack' storage bucket exists
INSERT INTO storage.buckets (id, name, public)
VALUES ('ApplyTrack', 'ApplyTrack', true)
ON CONFLICT (id) DO NOTHING;

-- 2. Allow authenticated users to upload attachments to their own user directory
-- Target path pattern: users/{userId}/{type}/{fileName}
CREATE POLICY "Allow authenticated user uploads to own directory"
ON storage.objects
FOR INSERT
TO authenticated
WITH CHECK (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[2] = auth.uid()::text
);

-- 3. Allow authenticated users to update/overwrite files in their own user directory
CREATE POLICY "Allow authenticated user updates to own directory"
ON storage.objects
FOR UPDATE
TO authenticated
USING (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[2] = auth.uid()::text
);

-- 4. Allow authenticated users to delete files from their own user directory
CREATE POLICY "Allow authenticated user deletes from own directory"
ON storage.objects
FOR DELETE
TO authenticated
USING (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[2] = auth.uid()::text
);

-- 5. Allow users to read/download attachments
CREATE POLICY "Allow authenticated user reads from own directory"
ON storage.objects
FOR SELECT
TO authenticated
USING (
  bucket_id = 'ApplyTrack'
  AND (storage.foldername(name))[2] = auth.uid()::text
);
