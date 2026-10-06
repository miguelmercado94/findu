package com.findu.help.v2.domain.model.priority;

import com.findu.help.v2.domain.enums.PriorityLevel;

public interface PriorityCalculator {
    int calculateScore(PriorityLevel level);
}
