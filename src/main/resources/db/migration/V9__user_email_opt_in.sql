alter table app.user_profile
    add column email_opt_in boolean not null default false;
