package com.davidpe.scapeai.application;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SessionRandomSource {

  private final AtomicLong effectiveSeed;
  private final AtomicReference<Random> sharedRandom;

  public SessionRandomSource(@Value("${scape.ai.random-seed:20260309}") long initialSeed) {
    this.effectiveSeed = new AtomicLong(initialSeed);
    this.sharedRandom = new AtomicReference<>(new Random(initialSeed));
  }

  public synchronized long resolveAndApplySeed(Long requestedSeed) {
    long seed = requestedSeed == null ? System.currentTimeMillis() : requestedSeed;
    reset(seed);
    return seed;
  }

  public synchronized void reset(long seed) {
    effectiveSeed.set(seed);
    sharedRandom.set(new Random(seed));
  }

  public long effectiveSeed() {
    return effectiveSeed.get();
  }

  public Random random() {
    return sharedRandom.get();
  }
}
