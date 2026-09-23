package com.goldwithdrawamount;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Large outlined text, like the original overlay but drawn inside its own bounds
 * so it can be moved with Alt + drag and never gets clipped.
 */
class GoldTextOverlay extends Overlay
{
    private final GoldWithdrawAmountPlugin plugin;
    private final GoldWithdrawAmountConfig config;

    // Deriving a font is relatively expensive, so only do it when the size setting changes.
    private Font cachedFont;
    private int cachedSize = -1;

    @Inject
    private GoldTextOverlay(GoldWithdrawAmountPlugin plugin, GoldWithdrawAmountConfig config)
    {
        super(plugin);
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        final GoldAlert alert = plugin.getCurrentAlert();
        final long now = System.currentTimeMillis();
        if (alert == null || alert.isExpired(now) || !config.overlayStyle().showsText())
        {
            return null;
        }

        graphics.setFont(getFont(config.textSize()));
        final FontMetrics metrics = graphics.getFontMetrics();
        final String text = alert.getShortText();

        // Draw from (1, ascent + 1) so the 1px outline stays inside the returned bounds.
        final int x = 1;
        final int y = metrics.getAscent() + 1;

        final Composite original = graphics.getComposite();
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alert.opacity(now)));
        try
        {
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, x - 1, y);
            graphics.drawString(text, x + 1, y);
            graphics.drawString(text, x, y - 1);
            graphics.drawString(text, x, y + 1);
            graphics.drawString(text, x + 1, y + 1);

            graphics.setColor(alert.getColor());
            graphics.drawString(text, x, y);
        }
        finally
        {
            graphics.setComposite(original);
        }

        return new Dimension(metrics.stringWidth(text) + 2, metrics.getHeight() + 2);
    }

    private Font getFont(int size)
    {
        if (cachedFont == null || cachedSize != size)
        {
            cachedFont = FontManager.getRunescapeBoldFont().deriveFont((float) size);
            cachedSize = size;
        }
        return cachedFont;
    }
}