package com.goldwithdrawamount;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup(GoldWithdrawAmountConfig.GROUP)
public interface GoldWithdrawAmountConfig extends Config
{
    String GROUP = "goldwithdrawamount";

    @ConfigSection(
            name = "Bank withdrawals",
            description = "Warnings when taking coins out of the bank",
            position = 0
    )
    String withdrawSection = "withdrawSection";

    @ConfigSection(
            name = "Gold drops",
            description = "Notifications for coins dropped by NPCs/players and coins you pick up",
            position = 1
    )
    String dropSection = "dropSection";

    @ConfigSection(
            name = "Display",
            description = "Chat message and overlay settings",
            position = 2
    )
    String displaySection = "displaySection";

    // ------------------------------------------------------------------ Bank withdrawals
    // keyName "enabled" and "threshold" are kept from the original version so existing settings carry over.

    @ConfigItem(
            keyName = "enabled",
            name = "Warn on large withdrawals",
            description = "Warn when you withdraw at least the threshold amount of coins from your bank",
            position = 0,
            section = withdrawSection
    )
    default boolean withdrawWarning()
    {
        return true;
    }

    @ConfigItem(
            keyName = "threshold",
            name = "Withdrawal threshold",
            description = "Warn when withdrawing this many coins or more in one go",
            position = 1,
            section = withdrawSection
    )
    @Range(min = 1)
    default int withdrawThreshold()
    {
        return 10000;
    }

    // ------------------------------------------------------------------ Gold drops

    @ConfigItem(
            keyName = "notifyDrops",
            name = "Notify for gold drops",
            description = "Notify when an NPC you kill (e.g. a boss) or a player you kill (PKing) drops coins at or above the drop threshold",
            position = 0,
            section = dropSection
    )
    default boolean notifyDrops()
    {
        return false;
    }

    @ConfigItem(
            keyName = "dropThreshold",
            name = "Drop threshold",
            description = "Minimum coins in a single kill's loot to notify",
            position = 1,
            section = dropSection
    )
    @Range(min = 1)
    default int dropThreshold()
    {
        return 100000;
    }

    @ConfigItem(
            keyName = "notifyPickups",
            name = "Notify for gold pickups",
            description = "Notify when you pick coins up off the ground at or above the pickup threshold",
            position = 2,
            section = dropSection
    )
    default boolean notifyPickups()
    {
        return false;
    }

    @ConfigItem(
            keyName = "pickupThreshold",
            name = "Pickup threshold",
            description = "Minimum coins picked up in one go to notify",
            position = 3,
            section = dropSection
    )
    @Range(min = 1)
    default int pickupThreshold()
    {
        return 100000;
    }

    // ------------------------------------------------------------------ Display

    @ConfigItem(
            keyName = "chatMessage",
            name = "Chat message",
            description = "Post a message in the chatbox for each warning",
            position = 0,
            section = displaySection
    )
    default boolean chatMessage()
    {
        return true;
    }

    @ConfigItem(
            keyName = "overlayStyle",
            name = "Overlay style",
            description = "Panel box (movable, like other RuneLite overlays), floating text, both, or none",
            position = 1,
            section = displaySection
    )
    default OverlayStyle overlayStyle()
    {
        return OverlayStyle.PANEL;
    }

    @ConfigItem(
            keyName = "overlayDuration",
            name = "Overlay duration",
            description = "How long the overlay stays on screen (fades out over the last second)",
            position = 2,
            section = displaySection
    )
    @Units(Units.SECONDS)
    @Range(min = 1, max = 60)
    default int overlayDuration()
    {
        return 8;
    }

    @ConfigItem(
            keyName = "textSize",
            name = "Floating text size",
            description = "Font size of the floating text overlay",
            position = 3,
            section = displaySection
    )
    @Range(min = 12, max = 48)
    default int textSize()
    {
        return 20;
    }

    @Alpha
    @ConfigItem(
            keyName = "withdrawColor",
            name = "Withdrawal colour",
            description = "Colour used for withdrawal warnings",
            position = 4,
            section = displaySection
    )
    default Color withdrawColor()
    {
        return new Color(255, 60, 60);
    }

    @Alpha
    @ConfigItem(
            keyName = "dropColor",
            name = "Drop / pickup colour",
            description = "Colour used for gold drop and pickup notifications",
            position = 5,
            section = displaySection
    )
    default Color dropColor()
    {
        return new Color(255, 200, 40);
    }
}