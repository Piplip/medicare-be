package com.nkd.medicare.domain;

import com.nkd.medicare.enums.FeedbackCategory;
import com.nkd.medicare.enums.FeedbackLevel;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FeedbackDTO {

    private FeedbackCategory category;
    private String content;
    private FeedbackLevel level;
}
