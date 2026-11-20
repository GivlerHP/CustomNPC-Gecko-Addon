package com.goodbird.npcgecko.entity;

/** Filters the ordinary look direction in degrees before it is applied to the head bone. */
final class NpcHeadPose {
    private static final float DEGREES_PER_SECOND = 180;
    private boolean initialized;
    private long updatedAt;
    float yaw;
    float pitch;

    void track(float targetYaw, float targetPitch, long now) {
        targetYaw = limit(targetYaw, 75);
        targetPitch = limit(targetPitch, 70);
        if (!initialized) {
            remember(targetYaw, targetPitch, now);
            return;
        }
        float seconds = Math.max(0, Math.min(50_000_000L, now - updatedAt)) / 1_000_000_000F;
        float maxChange = DEGREES_PER_SECOND * seconds;
        yaw += clip(targetYaw - yaw, maxChange);
        pitch += clip(targetPitch - pitch, maxChange);
        updatedAt = now;
    }

    void remember(float yaw, float pitch, long now) {
        this.yaw = limit(yaw, 75);
        this.pitch = limit(pitch, 70);
        initialized = true;
        updatedAt = now;
    }

    private static float limit(float angle, float max) {
        if (!Float.isFinite(angle)) return 0;
        float wrapped = angle % 360;
        if (wrapped >= 180) wrapped -= 360;
        if (wrapped < -180) wrapped += 360;
        return clip(wrapped, max);
    }

    private static float clip(float value, float limit) {
        return Math.max(-limit, Math.min(limit, value));
    }
}
