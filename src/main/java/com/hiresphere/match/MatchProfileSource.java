package com.hiresphere.match;

public interface MatchProfileSource {

    CandidateFacts factsOf(Long userId);
}