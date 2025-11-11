package com.goldwithdrawamount;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.ChatMessageType;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
        name = "Gold Withdraw Warning",
        description = "Warns you when withdrawing more than your set gold threshold.",
        tags = {"gold", "bank", "warning", "ironman"}
)
public class GoldWithdrawAmountPlugin extends Plugin
{
    @Inject
    private Client client;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private GoldWithdrawAmountOverlay overlay;

    @Inject
    private GoldWithdrawAmountConfig config;

    private int lastBankGold = 0;
    private int lastInventoryGold = 0;

    @Provides
    GoldWithdrawAmountConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GoldWithdrawAmountConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("Gold Withdraw Amount plugin started");
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown()
    {
        log.info("Gold Withdraw Amount plugin stopped");
        overlayManager.remove(overlay);
        lastBankGold = 0;
        lastInventoryGold = 0;
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.enabled())
        {
            return;
        }

        ItemContainer container = event.getItemContainer();

        if (container == null)
        {
            return;
        }

        if (event.getContainerId() == InventoryID.BANK.getId())
        {
            lastBankGold = getGoldAmount(container);
        }

        if (event.getContainerId() == InventoryID.INVENTORY.getId())
        {
            int currentInventoryGold = getGoldAmount(container);

            if (currentInventoryGold > lastInventoryGold && lastBankGold > 0)
            {
                int withdrawn = currentInventoryGold - lastInventoryGold;
                if (withdrawn >= config.threshold())
                {
                    sendWarning(withdrawn);
                }
            }

            lastInventoryGold = currentInventoryGold;
        }
    }

    private int getGoldAmount(ItemContainer container)
    {
        int total = 0;

        if (container == null || container.getItems() == null)
        {
            return 0;
        }

        for (net.runelite.api.Item item : container.getItems())
        {
            if (item.getId() == ItemID.COINS_995)
            {
                total += item.getQuantity();
            }
        }

        return total;
    }

    private void sendWarning(int amount)
    {
        String msg = "<col=ff0000>Warning: You withdrew " + amount +
                " coins! (Threshold: " + config.threshold() + ")</col>";


        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.CONSOLE)
                        .runeLiteFormattedMessage(msg)
                        .build()
        );


        chatMessageManager.queue(
                QueuedMessage.builder()
                        .type(ChatMessageType.GAMEMESSAGE)
                        .runeLiteFormattedMessage(msg)
                        .build()
        );


        overlay.trigger();
    }
}
