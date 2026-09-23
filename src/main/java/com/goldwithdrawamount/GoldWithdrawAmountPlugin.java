package com.goldwithdrawamount;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.Collection;
import javax.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.events.PlayerLootReceived;
import net.runelite.client.game.ItemStack;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.QuantityFormatter;

@Slf4j
@PluginDescriptor(
        name = "Gold Withdraw Warning",
        description = "Warns you when withdrawing more than your set gold threshold, with optional notifications for large gold drops and pickups.",
        tags = {"gold", "coins", "bank", "warning", "ironman", "drop", "loot", "pk"}
)
public class GoldWithdrawAmountPlugin extends Plugin
{
    /** Container changes and ground-item despawns for the same action arrive within this many ticks of each other. */
    private static final int MATCH_WINDOW_TICKS = 1;

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ChatMessageManager chatMessageManager;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private GoldPanelOverlay panelOverlay;

    @Inject
    private GoldTextOverlay textOverlay;

    @Inject
    private GoldWithdrawAmountConfig config;

    /** The alert currently shown by the overlays (null when nothing to show). */
    @Getter
    private volatile GoldAlert currentAlert;

    // Last known coin totals; -1 means "unknown", so the first update only sets a baseline.
    private long lastInventoryGold = -1;
    private long lastBankGold = -1;

    // Half-matched events waiting for their partner (withdrawal = bank loss + inventory gain,
    // pickup = coins despawning next to you + inventory gain).
    private long pendingBankLoss;
    private int pendingBankLossTick;
    private long pendingBankGain;
    private int pendingBankGainTick;
    private long pendingWorldGain;
    private int pendingWorldGainTick;
    private long pendingDespawnCoins;
    private int pendingDespawnTick;

    @Provides
    GoldWithdrawAmountConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GoldWithdrawAmountConfig.class);
    }

    @Override
    protected void startUp()
    {
        overlayManager.add(panelOverlay);
        overlayManager.add(textOverlay);

        clientThread.invoke(() ->
        {
            reset();
            if (client.getGameState() == GameState.LOGGED_IN)
            {
                lastInventoryGold = countGold(client.getItemContainer(InventoryID.INV));
                ItemContainer bank = client.getItemContainer(InventoryID.BANK);
                if (bank != null)
                {
                    lastBankGold = countGold(bank);
                }
            }
        });
    }

    @Override
    protected void shutDown()
    {
        overlayManager.remove(panelOverlay);
        overlayManager.remove(textOverlay);
        currentAlert = null;
        clientThread.invoke(this::reset);
    }

    private void reset()
    {
        lastInventoryGold = -1;
        lastBankGold = -1;
        clearPending();
    }

    private void clearPending()
    {
        pendingBankLoss = 0;
        pendingBankGain = 0;
        pendingWorldGain = 0;
        pendingDespawnCoins = 0;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        switch (event.getGameState())
        {
            case LOGIN_SCREEN:
            case HOPPING:
            case CONNECTION_LOST:
                // Containers are re-sent after login/hop; don't mistake that for gaining coins.
                reset();
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ Withdrawals and pickups

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        final int containerId = event.getContainerId();
        final int tick = client.getTickCount();

        if (containerId == InventoryID.BANK)
        {
            final long gold = countGold(event.getItemContainer());
            if (lastBankGold >= 0 && gold < lastBankGold)
            {
                pendingBankLoss += lastBankGold - gold;
                pendingBankLossTick = tick;
                tryMatchWithdrawal();
            }
            lastBankGold = gold;
        }
        else if (containerId == InventoryID.INV)
        {
            final long gold = countGold(event.getItemContainer());
            if (lastInventoryGold >= 0 && gold > lastInventoryGold)
            {
                final long gain = gold - lastInventoryGold;
                if (isBankOpen())
                {
                    pendingBankGain += gain;
                    pendingBankGainTick = tick;
                    tryMatchWithdrawal();
                }
                else
                {
                    pendingWorldGain += gain;
                    pendingWorldGainTick = tick;
                    tryMatchPickup();
                }
            }
            lastInventoryGold = gold;
        }
    }

    @Subscribe
    public void onItemDespawned(ItemDespawned event)
    {
        if (event.getItem().getId() != ItemID.COINS)
        {
            return;
        }

        final Player player = client.getLocalPlayer();
        if (player == null || event.getTile().getWorldLocation().distanceTo(player.getWorldLocation()) > 1)
        {
            return; // not a stack you could have just picked up
        }

        pendingDespawnCoins += event.getItem().getQuantity();
        pendingDespawnTick = client.getTickCount();
        tryMatchPickup();
    }

    /**
     * A withdrawal is only counted when the bank loses coins AND the inventory gains coins
     * at the same time with the bank open. Shops, trades, pickups and logins can't trigger it.
     */
    private void tryMatchWithdrawal()
    {
        expireStale();
        if (pendingBankLoss <= 0 || pendingBankGain <= 0)
        {
            return;
        }

        final long amount = Math.min(pendingBankLoss, pendingBankGain);
        pendingBankLoss = 0;
        pendingBankGain = 0;

        if (config.withdrawWarning() && amount >= config.withdrawThreshold())
        {
            showAlert(GoldAlert.Type.WITHDRAWAL, "Large withdrawal",
                    "Withdrew " + QuantityFormatter.quantityToStackSize(amount) + " coins!",
                    amount, config.withdrawThreshold(), null, config.withdrawColor());
        }
    }

    /**
     * A pickup is counted when the inventory gains coins (bank closed) at the same time as
     * a coin stack disappears next to you.
     */
    private void tryMatchPickup()
    {
        expireStale();
        if (pendingWorldGain <= 0 || pendingDespawnCoins <= 0)
        {
            return;
        }

        final long amount = pendingWorldGain;
        pendingWorldGain = 0;
        pendingDespawnCoins = 0;

        if (config.notifyPickups() && amount >= config.pickupThreshold())
        {
            showAlert(GoldAlert.Type.PICKUP, "Gold picked up",
                    "Picked up " + QuantityFormatter.quantityToStackSize(amount) + " coins!",
                    amount, config.pickupThreshold(), null, config.dropColor());
        }
    }

    private void expireStale()
    {
        final int now = client.getTickCount();
        if (now - pendingBankLossTick > MATCH_WINDOW_TICKS)
        {
            pendingBankLoss = 0;
        }
        if (now - pendingBankGainTick > MATCH_WINDOW_TICKS)
        {
            pendingBankGain = 0;
        }
        if (now - pendingWorldGainTick > MATCH_WINDOW_TICKS)
        {
            pendingWorldGain = 0;
        }
        if (now - pendingDespawnTick > MATCH_WINDOW_TICKS)
        {
            pendingDespawnCoins = 0;
        }
    }

    private boolean isBankOpen()
    {
        final Widget bank = client.getWidget(InterfaceID.Bankmain.UNIVERSE);
        return bank != null && !bank.isHidden();
    }

    // ------------------------------------------------------------------ Kill loot (bosses / PKing)

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived event)
    {
        final String name = event.getNpc() != null ? event.getNpc().getName() : null;
        handleLoot(event.getItems(), name);
    }

    @Subscribe
    public void onPlayerLootReceived(PlayerLootReceived event)
    {
        final String name = event.getPlayer() != null ? event.getPlayer().getName() : null;
        handleLoot(event.getItems(), name);
    }

    private void handleLoot(Collection<ItemStack> items, String sourceName)
    {
        if (!config.notifyDrops())
        {
            return;
        }

        long coins = 0;
        for (ItemStack stack : items)
        {
            if (stack.getId() == ItemID.COINS)
            {
                coins += stack.getQuantity();
            }
        }

        if (coins > 0 && coins >= config.dropThreshold())
        {
            showAlert(GoldAlert.Type.DROP, "Gold drop",
                    QuantityFormatter.quantityToStackSize(coins) + " coins dropped!",
                    coins, config.dropThreshold(), sourceName, config.dropColor());
        }
    }

    // ------------------------------------------------------------------ Output

    private void showAlert(GoldAlert.Type type, String title, String shortText,
                           long amount, long threshold, String source, Color color)
    {
        final long now = System.currentTimeMillis();
        currentAlert = new GoldAlert(type, title, shortText, amount, threshold, source, color,
                now, now + config.overlayDuration() * 1000L);

        if (config.chatMessage())
        {
            sendChat(type, amount, threshold, source, color);
        }
    }

    private void sendChat(GoldAlert.Type type, long amount, long threshold, String source, Color color)
    {
        final String coins = QuantityFormatter.formatNumber(amount);
        final String limit = QuantityFormatter.formatNumber(threshold);
        final String text;
        switch (type)
        {
            case WITHDRAWAL:
                text = "Warning: you withdrew " + coins + " coins (threshold " + limit + ").";
                break;
            case DROP:
                text = "Gold drop: " + coins + " coins" + (source != null ? " from " + source : "") + ".";
                break;
            case PICKUP:
            default:
                text = "You picked up " + coins + " coins.";
                break;
        }

        chatMessageManager.queue(QueuedMessage.builder()
                .type(ChatMessageType.GAMEMESSAGE)
                .runeLiteFormattedMessage(ColorUtil.wrapWithColorTag(text, color))
                .build());
    }

    private static long countGold(ItemContainer container)
    {
        if (container == null)
        {
            return 0;
        }

        long total = 0;
        for (Item item : container.getItems())
        {
            if (item.getId() == ItemID.COINS)
            {
                total += item.getQuantity();
            }
        }
        return total;
    }
}