-- Placeholder survey content: onboarding + the four themed surveys, each published
-- as version 1. IDs are deterministic (md5 of a stable label cast to uuid) so this
-- file reads without a separate id bookkeeping table and is safe to extend.
--
-- This is placeholder copy for scaffolding, not reviewed clinical content - expect
-- it to be superseded by a later V7__... migration once real survey copy exists.
-- Never edit this file after it has been applied anywhere.

-- =========================================================================
-- Surveys + published v1 versions
-- =========================================================================

insert into app.survey (id, code, theme, title, description, sort_order, is_active) values
    (md5('survey:onboarding')::uuid,   'onboarding',   'ONBOARDING',   'Getting to know you', 'A few questions so we can personalize your experience.', 0, true),
    (md5('survey:exercise')::uuid,     'exercise',     'EXERCISE',     'Exercise',             'Your movement and activity habits.', 1, true),
    (md5('survey:nutrition')::uuid,    'nutrition',    'NUTRITION',    'Nutrition',            'Your eating patterns and diet.', 2, true),
    (md5('survey:hormones')::uuid,     'hormones',     'HORMONES',     'Hormones & Cycle',     'Your menstrual cycle and hormonal health.', 3, true),
    (md5('survey:pelvic_floor')::uuid, 'pelvic_floor', 'PELVIC_FLOOR', 'Pelvic Floor',         'Your pelvic floor health.', 4, true);

insert into app.survey_version (id, survey_id, version, status, published_at, notes) values
    (md5('survey_version:onboarding:1')::uuid,   md5('survey:onboarding')::uuid,   1, 'PUBLISHED', now(), 'Initial placeholder version'),
    (md5('survey_version:exercise:1')::uuid,     md5('survey:exercise')::uuid,     1, 'PUBLISHED', now(), 'Initial placeholder version'),
    (md5('survey_version:nutrition:1')::uuid,    md5('survey:nutrition')::uuid,    1, 'PUBLISHED', now(), 'Initial placeholder version'),
    (md5('survey_version:hormones:1')::uuid,     md5('survey:hormones')::uuid,     1, 'PUBLISHED', now(), 'Initial placeholder version'),
    (md5('survey_version:pelvic_floor:1')::uuid, md5('survey:pelvic_floor')::uuid, 1, 'PUBLISHED', now(), 'Initial placeholder version');

insert into app.survey_section (id, survey_version_id, code, title, sort_order) values
    (md5('section:onboarding:general')::uuid,   md5('survey_version:onboarding:1')::uuid,   'general', 'General', 0),
    (md5('section:exercise:general')::uuid,     md5('survey_version:exercise:1')::uuid,     'general', 'General', 0),
    (md5('section:nutrition:general')::uuid,    md5('survey_version:nutrition:1')::uuid,    'general', 'General', 0),
    (md5('section:hormones:general')::uuid,     md5('survey_version:hormones:1')::uuid,     'general', 'General', 0),
    (md5('section:pelvic_floor:general')::uuid, md5('survey_version:pelvic_floor:1')::uuid, 'general', 'General', 0);

-- =========================================================================
-- Onboarding
-- =========================================================================

insert into app.question (id, survey_version_id, section_id, code, type, prompt, help_text, required, sort_order, config) values
    (md5('question:onboarding:age_range')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'age_range', 'SINGLE_CHOICE', 'What is your age range?', null, true, 0, '{}'),
    (md5('question:onboarding:goals')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'goals', 'MULTI_CHOICE', 'What are you hoping to improve?', 'Select all that apply.', true, 1, '{}'),
    (md5('question:onboarding:height_cm')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'height_cm', 'NUMBER', 'What is your height?', null, false, 2, '{"min":120,"max":220,"unit":"cm"}'),
    (md5('question:onboarding:weight_kg')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'weight_kg', 'NUMBER', 'What is your weight?', null, false, 3, '{"min":30,"max":250,"unit":"kg"}'),
    (md5('question:onboarding:takes_medication')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'takes_medication', 'BOOLEAN', 'Are you currently taking any medication?', null, false, 4, '{}'),
    (md5('question:onboarding:stress_level')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'stress_level', 'SCALE', 'How would you rate your typical stress level?', null, true, 5,
     '{"min":1,"max":5,"scaleLabels":{"1":"Very low","5":"Very high"}}'),
    (md5('question:onboarding:free_text')::uuid, md5('survey_version:onboarding:1')::uuid, md5('section:onboarding:general')::uuid,
     'free_text', 'LONG_TEXT', 'Anything else you would like us to know about your health?',
     'Optional - share whatever feels relevant.', false, 6, '{"maxLength":2000}');

insert into app.question_option (id, question_id, code, label, sort_order) values
    (md5('option:onboarding:age_range:18_24')::uuid, md5('question:onboarding:age_range')::uuid, '18_24', '18-24', 0),
    (md5('option:onboarding:age_range:25_34')::uuid, md5('question:onboarding:age_range')::uuid, '25_34', '25-34', 1),
    (md5('option:onboarding:age_range:35_44')::uuid, md5('question:onboarding:age_range')::uuid, '35_44', '35-44', 2),
    (md5('option:onboarding:age_range:45_54')::uuid, md5('question:onboarding:age_range')::uuid, '45_54', '45-54', 3),
    (md5('option:onboarding:age_range:55_plus')::uuid, md5('question:onboarding:age_range')::uuid, '55_plus', '55+', 4);

insert into app.question_option (id, question_id, code, label, sort_order, metadata) values
    (md5('option:onboarding:goals:energy')::uuid, md5('question:onboarding:goals')::uuid, 'energy', 'More energy', 0, '{}'),
    (md5('option:onboarding:goals:weight')::uuid, md5('question:onboarding:goals')::uuid, 'weight', 'Weight management', 1, '{}'),
    (md5('option:onboarding:goals:hormones')::uuid, md5('question:onboarding:goals')::uuid, 'hormones', 'Hormonal balance', 2, '{"signals":["HORMONES_FOCUS"]}'),
    (md5('option:onboarding:goals:fitness')::uuid, md5('question:onboarding:goals')::uuid, 'fitness', 'Fitness', 3, '{}'),
    (md5('option:onboarding:goals:sleep')::uuid, md5('question:onboarding:goals')::uuid, 'sleep', 'Better sleep', 4, '{}'),
    (md5('option:onboarding:goals:pelvic_health')::uuid, md5('question:onboarding:goals')::uuid, 'pelvic_health', 'Pelvic health', 5, '{"signals":["PELVIC_FLOOR_FOCUS"]}');

-- =========================================================================
-- Exercise
-- =========================================================================

insert into app.question (id, survey_version_id, section_id, code, type, prompt, required, sort_order, config) values
    (md5('question:exercise:frequency')::uuid, md5('survey_version:exercise:1')::uuid, md5('section:exercise:general')::uuid,
     'frequency', 'SINGLE_CHOICE', 'How often do you currently exercise?', true, 0, '{}'),
    (md5('question:exercise:types')::uuid, md5('survey_version:exercise:1')::uuid, md5('section:exercise:general')::uuid,
     'types', 'MULTI_CHOICE', 'Which types of exercise do you do?', false, 1, '{}'),
    (md5('question:exercise:session_minutes')::uuid, md5('survey_version:exercise:1')::uuid, md5('section:exercise:general')::uuid,
     'session_minutes', 'NUMBER', 'How many minutes does a typical session last?', false, 2, '{"min":0,"max":300,"unit":"min"}'),
    (md5('question:exercise:enjoyment')::uuid, md5('survey_version:exercise:1')::uuid, md5('section:exercise:general')::uuid,
     'enjoyment', 'SCALE', 'How much do you enjoy exercising?', true, 3,
     '{"min":1,"max":5,"scaleLabels":{"1":"Not at all","5":"Love it"}}'),
    (md5('question:exercise:limitations')::uuid, md5('survey_version:exercise:1')::uuid, md5('section:exercise:general')::uuid,
     'limitations', 'TEXT', 'Any injuries or physical limitations we should know about?', false, 4, '{"maxLength":300}');

insert into app.question_option (id, question_id, code, label, sort_order) values
    (md5('option:exercise:frequency:none')::uuid, md5('question:exercise:frequency')::uuid, 'none', 'None', 0),
    (md5('option:exercise:frequency:1_2_week')::uuid, md5('question:exercise:frequency')::uuid, '1_2_week', '1-2 times/week', 1),
    (md5('option:exercise:frequency:3_4_week')::uuid, md5('question:exercise:frequency')::uuid, '3_4_week', '3-4 times/week', 2),
    (md5('option:exercise:frequency:5_plus_week')::uuid, md5('question:exercise:frequency')::uuid, '5_plus_week', '5+ times/week', 3),
    (md5('option:exercise:types:cardio')::uuid, md5('question:exercise:types')::uuid, 'cardio', 'Cardio', 0),
    (md5('option:exercise:types:strength')::uuid, md5('question:exercise:types')::uuid, 'strength', 'Strength training', 1),
    (md5('option:exercise:types:yoga')::uuid, md5('question:exercise:types')::uuid, 'yoga', 'Yoga', 2),
    (md5('option:exercise:types:pilates')::uuid, md5('question:exercise:types')::uuid, 'pilates', 'Pilates', 3),
    (md5('option:exercise:types:walking')::uuid, md5('question:exercise:types')::uuid, 'walking', 'Walking', 4),
    (md5('option:exercise:types:none')::uuid, md5('question:exercise:types')::uuid, 'none', 'None', 5);

-- =========================================================================
-- Nutrition
-- =========================================================================

insert into app.question (id, survey_version_id, section_id, code, type, prompt, required, sort_order, config) values
    (md5('question:nutrition:diet_pattern')::uuid, md5('survey_version:nutrition:1')::uuid, md5('section:nutrition:general')::uuid,
     'diet_pattern', 'SINGLE_CHOICE', 'Which best describes your diet?', true, 0, '{}'),
    (md5('question:nutrition:meals_per_day')::uuid, md5('survey_version:nutrition:1')::uuid, md5('section:nutrition:general')::uuid,
     'meals_per_day', 'NUMBER', 'How many meals do you eat per day?', false, 1, '{"min":1,"max":10}'),
    (md5('question:nutrition:water_liters')::uuid, md5('survey_version:nutrition:1')::uuid, md5('section:nutrition:general')::uuid,
     'water_liters', 'NUMBER', 'How many liters of water do you drink daily?', false, 2, '{"min":0,"max":10,"unit":"L"}'),
    (md5('question:nutrition:supplements')::uuid, md5('survey_version:nutrition:1')::uuid, md5('section:nutrition:general')::uuid,
     'supplements', 'MULTI_CHOICE', 'Which supplements do you currently take?', false, 3, '{}'),
    (md5('question:nutrition:food_notes')::uuid, md5('survey_version:nutrition:1')::uuid, md5('section:nutrition:general')::uuid,
     'food_notes', 'LONG_TEXT', 'Anything else about your eating habits we should know?', false, 4, '{"maxLength":2000}');

insert into app.question_option (id, question_id, code, label, sort_order) values
    (md5('option:nutrition:diet_pattern:omnivore')::uuid, md5('question:nutrition:diet_pattern')::uuid, 'omnivore', 'Omnivore', 0),
    (md5('option:nutrition:diet_pattern:vegetarian')::uuid, md5('question:nutrition:diet_pattern')::uuid, 'vegetarian', 'Vegetarian', 1),
    (md5('option:nutrition:diet_pattern:vegan')::uuid, md5('question:nutrition:diet_pattern')::uuid, 'vegan', 'Vegan', 2),
    (md5('option:nutrition:diet_pattern:pescatarian')::uuid, md5('question:nutrition:diet_pattern')::uuid, 'pescatarian', 'Pescatarian', 3),
    (md5('option:nutrition:diet_pattern:other')::uuid, md5('question:nutrition:diet_pattern')::uuid, 'other', 'Other', 4);

insert into app.question_option (id, question_id, code, label, sort_order, metadata) values
    (md5('option:nutrition:supplements:iron')::uuid, md5('question:nutrition:supplements')::uuid, 'iron', 'Iron', 0, '{"signals":["IRON_SUPPLEMENTED"]}'),
    (md5('option:nutrition:supplements:vitamin_d')::uuid, md5('question:nutrition:supplements')::uuid, 'vitamin_d', 'Vitamin D', 1, '{}'),
    (md5('option:nutrition:supplements:b12')::uuid, md5('question:nutrition:supplements')::uuid, 'b12', 'Vitamin B12', 2, '{}'),
    (md5('option:nutrition:supplements:omega_3')::uuid, md5('question:nutrition:supplements')::uuid, 'omega_3', 'Omega-3', 3, '{}'),
    (md5('option:nutrition:supplements:none')::uuid, md5('question:nutrition:supplements')::uuid, 'none', 'None', 4, '{}');

-- =========================================================================
-- Hormones
-- =========================================================================

insert into app.question (id, survey_version_id, section_id, code, type, prompt, required, sort_order, config) values
    (md5('question:hormones:cycle_regularity')::uuid, md5('survey_version:hormones:1')::uuid, md5('section:hormones:general')::uuid,
     'cycle_regularity', 'SINGLE_CHOICE', 'How regular is your menstrual cycle?', true, 0, '{}'),
    (md5('question:hormones:last_period_date')::uuid, md5('survey_version:hormones:1')::uuid, md5('section:hormones:general')::uuid,
     'last_period_date', 'DATE', 'When did your last period start?', false, 1, '{}'),
    (md5('question:hormones:symptoms')::uuid, md5('survey_version:hormones:1')::uuid, md5('section:hormones:general')::uuid,
     'symptoms', 'MULTI_CHOICE', 'Which symptoms do you regularly experience?', false, 2, '{}'),
    (md5('question:hormones:on_birth_control')::uuid, md5('survey_version:hormones:1')::uuid, md5('section:hormones:general')::uuid,
     'on_birth_control', 'BOOLEAN', 'Are you currently using hormonal birth control?', false, 3, '{}'),
    (md5('question:hormones:symptom_severity')::uuid, md5('survey_version:hormones:1')::uuid, md5('section:hormones:general')::uuid,
     'symptom_severity', 'SCALE', 'How severe are your symptoms overall?', false, 4,
     '{"min":1,"max":5,"scaleLabels":{"1":"Mild","5":"Severe"}}');

insert into app.question_option (id, question_id, code, label, sort_order) values
    (md5('option:hormones:cycle_regularity:regular')::uuid, md5('question:hormones:cycle_regularity')::uuid, 'regular', 'Regular', 0),
    (md5('option:hormones:cycle_regularity:irregular')::uuid, md5('question:hormones:cycle_regularity')::uuid, 'irregular', 'Irregular', 1),
    (md5('option:hormones:cycle_regularity:no_periods')::uuid, md5('question:hormones:cycle_regularity')::uuid, 'no_periods', 'No periods', 2),
    (md5('option:hormones:cycle_regularity:unsure')::uuid, md5('question:hormones:cycle_regularity')::uuid, 'unsure', 'Not sure', 3);

insert into app.question_option (id, question_id, code, label, sort_order, metadata) values
    (md5('option:hormones:symptoms:cramps')::uuid, md5('question:hormones:symptoms')::uuid, 'cramps', 'Cramps', 0, '{}'),
    (md5('option:hormones:symptoms:mood_swings')::uuid, md5('question:hormones:symptoms')::uuid, 'mood_swings', 'Mood swings', 1, '{"signals":["MOOD_SWINGS"]}'),
    (md5('option:hormones:symptoms:bloating')::uuid, md5('question:hormones:symptoms')::uuid, 'bloating', 'Bloating', 2, '{}'),
    (md5('option:hormones:symptoms:acne')::uuid, md5('question:hormones:symptoms')::uuid, 'acne', 'Acne', 3, '{}'),
    (md5('option:hormones:symptoms:fatigue')::uuid, md5('question:hormones:symptoms')::uuid, 'fatigue', 'Fatigue', 4, '{"signals":["FATIGUE"]}'),
    (md5('option:hormones:symptoms:none')::uuid, md5('question:hormones:symptoms')::uuid, 'none', 'None', 5, '{}');

-- =========================================================================
-- Pelvic floor
-- =========================================================================

insert into app.question (id, survey_version_id, section_id, code, type, prompt, required, sort_order, config) values
    (md5('question:pelvic_floor:leakage_frequency')::uuid, md5('survey_version:pelvic_floor:1')::uuid, md5('section:pelvic_floor:general')::uuid,
     'leakage_frequency', 'SINGLE_CHOICE', 'How often do you experience bladder leakage?', true, 0, '{}'),
    (md5('question:pelvic_floor:symptoms')::uuid, md5('survey_version:pelvic_floor:1')::uuid, md5('section:pelvic_floor:general')::uuid,
     'symptoms', 'MULTI_CHOICE', 'Which pelvic floor symptoms do you experience?', false, 1, '{}'),
    (md5('question:pelvic_floor:kegel_frequency')::uuid, md5('survey_version:pelvic_floor:1')::uuid, md5('section:pelvic_floor:general')::uuid,
     'kegel_frequency', 'SINGLE_CHOICE', 'How often do you do pelvic floor (Kegel) exercises?', false, 2, '{}'),
    (md5('question:pelvic_floor:childbirth_history')::uuid, md5('survey_version:pelvic_floor:1')::uuid, md5('section:pelvic_floor:general')::uuid,
     'childbirth_history', 'BOOLEAN', 'Have you given birth vaginally?', false, 3, '{}'),
    (md5('question:pelvic_floor:discomfort_level')::uuid, md5('survey_version:pelvic_floor:1')::uuid, md5('section:pelvic_floor:general')::uuid,
     'discomfort_level', 'SCALE', 'How would you rate your pelvic discomfort?', false, 4,
     '{"min":1,"max":5,"scaleLabels":{"1":"None","5":"Severe"}}');

insert into app.question_option (id, question_id, code, label, sort_order) values
    (md5('option:pelvic_floor:leakage_frequency:never')::uuid, md5('question:pelvic_floor:leakage_frequency')::uuid, 'never', 'Never', 0),
    (md5('option:pelvic_floor:leakage_frequency:rarely')::uuid, md5('question:pelvic_floor:leakage_frequency')::uuid, 'rarely', 'Rarely', 1),
    (md5('option:pelvic_floor:leakage_frequency:sometimes')::uuid, md5('question:pelvic_floor:leakage_frequency')::uuid, 'sometimes', 'Sometimes', 2),
    (md5('option:pelvic_floor:leakage_frequency:often')::uuid, md5('question:pelvic_floor:leakage_frequency')::uuid, 'often', 'Often', 3),
    (md5('option:pelvic_floor:leakage_frequency:always')::uuid, md5('question:pelvic_floor:leakage_frequency')::uuid, 'always', 'Always', 4),
    (md5('option:pelvic_floor:kegel_frequency:never')::uuid, md5('question:pelvic_floor:kegel_frequency')::uuid, 'never', 'Never', 0),
    (md5('option:pelvic_floor:kegel_frequency:occasionally')::uuid, md5('question:pelvic_floor:kegel_frequency')::uuid, 'occasionally', 'Occasionally', 1),
    (md5('option:pelvic_floor:kegel_frequency:daily')::uuid, md5('question:pelvic_floor:kegel_frequency')::uuid, 'daily', 'Daily', 2),
    (md5('option:pelvic_floor:kegel_frequency:multiple_daily')::uuid, md5('question:pelvic_floor:kegel_frequency')::uuid, 'multiple_daily', 'Multiple times a day', 3);

insert into app.question_option (id, question_id, code, label, sort_order, metadata) values
    (md5('option:pelvic_floor:symptoms:leakage')::uuid, md5('question:pelvic_floor:symptoms')::uuid, 'leakage', 'Leakage', 0, '{"signals":["PELVIC_LEAKAGE"]}'),
    (md5('option:pelvic_floor:symptoms:urgency')::uuid, md5('question:pelvic_floor:symptoms')::uuid, 'urgency', 'Urgency', 1, '{}'),
    (md5('option:pelvic_floor:symptoms:pain')::uuid, md5('question:pelvic_floor:symptoms')::uuid, 'pain', 'Pain', 2, '{}'),
    (md5('option:pelvic_floor:symptoms:pressure')::uuid, md5('question:pelvic_floor:symptoms')::uuid, 'pressure', 'Pressure or heaviness', 3, '{}'),
    (md5('option:pelvic_floor:symptoms:none')::uuid, md5('question:pelvic_floor:symptoms')::uuid, 'none', 'None', 4, '{}');
