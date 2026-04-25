-- Migrate exercises from fitsense (int id) to fitsensee (UUID id)
INSERT INTO fitsensee.exercise (id, nom, type, duree, description, sets, reps, image_name, updated_at, youtube_video_id)
SELECT 
    UNHEX(REPLACE(UUID(),'-','')),
    nom, type, duree, description, sets, reps, image_name, updated_at, youtube_video_id
FROM fitsense.exercise;
