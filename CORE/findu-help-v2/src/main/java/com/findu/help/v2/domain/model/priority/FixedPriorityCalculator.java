package com.findu.help.v2.domain.model.priority;

import com.findu.help.v2.domain.enums.PriorityLevel;

public class FixedPriorityCalculator implements PriorityCalculator {

    @Override
    public int calculateScore(PriorityLevel level) {
        if (level == null) {
            return PriorityLevel.MEDIA.getDefaultScore();
        }
        return level.getDefaultScore();
    }
}
