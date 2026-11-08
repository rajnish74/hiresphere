package com.hiresphere.ai.resilience;

import java.time.Duration;

@FunctionalInterface
public interface Sleeper {

    Sleeper THREAD = d -> Thread.sleep(d.toMillis());

    void sleep(Duration duration) throws InterruptedException;
}