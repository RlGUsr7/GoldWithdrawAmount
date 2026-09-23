package com.goldwithdrawamount;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.QuantityFormatter;

/**
 * Standard RuneLite info panel: movable (Alt + drag), resizable, and matches other plugins' overlays.
 */
class GoldPanelOverlay extends OverlayPanel
{
    private static final int PANEL_WIDTH = 160;

    private final GoldWithdrawAmountPlugin plugin;
    private final GoldWithdrawAmountConfig config;

    @Inject
    private GoldPanelOverlay(GoldWithdrawAmountPlugin plugin, GoldWithdrawAmountConfig config)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS); // stay visible on top of the bank interface
        panelComponent.setPreferredSize(new Dimension(PANEL_WIDTH, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final GoldAlert alert = plugin.getCurrentAlert();
        final long now = System.currentTimeMillis();
        if (alert == null || alert.isExpired(now) || !config.overlayStyle().showsPanel())
        {
            return null;
        }

        panelComponent.getChildren().add(TitleComponent.builder()
                .text(alert.getTitle())
                .color(alert.getColor())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Amount:")
                .right(QuantityFormatter.formatNumber(alert.getAmount()))
                .rightColor(alert.getColor())
                .build());

        if (alert.getSource() != null)
        {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("From:")
                    .right(alert.getSource())
                    .build());
        }

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Threshold:")
                .right(QuantityFormatter.formatNumber(alert.getThreshold()))
                .build());

        final Composite original = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alert.opacity(now)));
        try
        {
            return super.render(graphics);
        }
        finally
        {
            graphics.setComposite(original);
        }
    }
}