-- At most one active survey per theme. Two active surveys sharing a theme is exactly the bug
-- that let a draft-only survey silently eclipse the real one on the home page: the API returned
-- both, and the frontend's per-theme lookup picked whichever sorted first.
create unique index ux_survey_active_theme on app.survey (theme) where is_active;
