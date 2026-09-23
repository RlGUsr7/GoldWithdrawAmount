package com.goldwithdrawamount;

import java.awt.Color;
import lombok.Value;

/**
 * One active warning shown by the overlays.
 */
@Value
class GoldAlert
{
    enum Type
    {
        WITHDRAWAL,
        DROP,
        PICKUP
    }

    Type type;
    /** Short headline, e.g. "Large withdrawal" */
    String title;
    /** Floating-text line, e.g. "Withdrew 1.2M coins!" */
    String shortText;
    long amount;
    long threshold;
    /** Optional extra detail, e.g. the NPC or player the drop came from. May be null. */
    String source;
    Color color;
    long createdAtMs;
    long expiresAtMs;

    boolean isExpired(long now)
    {
        return now >= expiresAtMs;
    }

    /**
     * 1.0 while fully visible, fading linearly to 0.0 over the final second.
     */
    float opacity(long now)
    {
        long remaining = expiresAtMs - now;
        if (remaining >= 1000)
        {
            return 1f;
        }
        return Math.max(0f, remaining / 1000f);
    }
}