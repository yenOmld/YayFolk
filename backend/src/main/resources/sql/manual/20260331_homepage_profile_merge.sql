ALTER TABLE users
    ADD COLUMN signature VARCHAR(120) NULL AFTER bio,
    ADD COLUMN cover_photo VARCHAR(255) NULL AFTER shop_cover,
    ADD COLUMN collection_visibility VARCHAR(20) NOT NULL DEFAULT 'public' AFTER shop_intro;

ALTER TABLE travel_posts
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'public' AFTER tags;

UPDATE users
SET collection_visibility = 'public'
WHERE collection_visibility IS NULL OR collection_visibility = '';

UPDATE users
SET cover_photo = shop_cover
WHERE (cover_photo IS NULL OR cover_photo = '')
  AND shop_cover IS NOT NULL
  AND shop_cover <> '';

UPDATE travel_posts
SET visibility = 'public'
WHERE visibility IS NULL OR visibility = '';
