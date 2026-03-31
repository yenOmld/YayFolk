ALTER TABLE activities
    ADD COLUMN video_url VARCHAR(500) NULL AFTER images,
    ADD COLUMN video_cover_url VARCHAR(500) NULL AFTER video_url;