package com.ionres.respondph.common.services;

import java.util.concurrent.atomic.AtomicLong;

public final class NewsETAEstimator {

    private static final double ALPHA       = 0.25;
    private static final long   DEFAULT_MS  = 15_000L;   // 15 s safe first-run default
    private static final long   MIN_EMA_MS  = 3_000L;    // never estimate below 3 s
    private static final long   MAX_EMA_MS  = 120_000L;  // never estimate above 2 min

    private final AtomicLong emaMillis = new AtomicLong(0);

    public long getEstimatedDurationMs() {
        long ema = emaMillis.get();
        if (ema <= 0) return DEFAULT_MS;
        return Math.max(MIN_EMA_MS, Math.min(MAX_EMA_MS, ema));
    }

    public void recordSampleMillis(long millis) {
        if (millis <= 0) return;
        long clamped = Math.max(MIN_EMA_MS, Math.min(MAX_EMA_MS, millis));
        long cur     = emaMillis.get();
        if (cur <= 0) {
            emaMillis.set(clamped);
        } else {
            long next = (long) (cur + ALPHA * (clamped - cur));
            emaMillis.set(Math.max(1L, next));
        }
    }
}