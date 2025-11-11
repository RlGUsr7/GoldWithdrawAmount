package com.goldwithdrawamount;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("goldwithdrawamount")
public interface GoldWithdrawAmountConfig extends Config
{
    @ConfigItem(
            keyName = "enabled",
            name = "Enable warning",
            description = "Toggle the gold withdrawal warning on or off"
    )
    default boolean enabled()
    {
        return true;
    }

    @ConfigItem(
            keyName = "threshold",
            name = "Gold threshold",
            description = "Warn when withdrawing this amount of gold or more"
    )
    default int threshold()
    {
        return 10000;
    }
}
