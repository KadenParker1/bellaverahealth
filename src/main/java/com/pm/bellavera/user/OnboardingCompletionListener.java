package com.pm.bellavera.user;

import com.pm.bellavera.response.SurveyResponseSubmittedEvent;
import java.time.Instant;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Marks onboarding complete on the user's profile the first time they submit it. */
@Component
public class OnboardingCompletionListener {

    private static final String ONBOARDING_SURVEY_CODE = "onboarding";

    private final UserProfileRepository userProfileRepository;

    public OnboardingCompletionListener(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @EventListener
    @Transactional
    public void onSurveyResponseSubmitted(SurveyResponseSubmittedEvent event) {
        if (!ONBOARDING_SURVEY_CODE.equals(event.surveyCode())) {
            return;
        }
        userProfileRepository.findById(event.userId()).ifPresent(profile -> {
            if (profile.getOnboardingCompletedAt() == null) {
                profile.setOnboardingCompletedAt(Instant.now());
                userProfileRepository.save(profile);
            }
        });
    }
}
