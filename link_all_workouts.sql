-- Link the "Full body" template (id=28) to all workouts that don't have a template yet
INSERT IGNORE INTO questionnaire_workout (questionnaire_id, workout_id)
SELECT 28, w.id FROM workout w
WHERE w.id NOT IN (SELECT workout_id FROM questionnaire_workout WHERE questionnaire_id = 28);
