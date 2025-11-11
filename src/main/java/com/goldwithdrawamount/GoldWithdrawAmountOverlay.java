package com.goldwithdrawamount;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;
import java.time.Instant;

public class GoldWithdrawAmountOverlay extends Overlay
{
    private static final int DISPLAY_DURATION_MS = 10000;

    private final Client client;
    private Instant showUntil;

    @Inject
    private GoldWithdrawAmountOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    public void trigger()
    {
        showUntil = Instant.now().plusMillis(DISPLAY_DURATION_MS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (showUntil == null || Instant.now().isAfter(showUntil))
        {
            return null;
        }

        String text = "High gold withdrawal!";
        graphics.setFont(new Font("Arial", Font.BOLD, 18));


        FontMetrics metrics = graphics.getFontMetrics();
        int x = 10;
        int y = 50;

        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x - 1, y - 1);
        graphics.drawString(text, x + 1, y - 1);
        graphics.drawString(text, x - 1, y + 1);
        graphics.drawString(text, x + 1, y + 1);

        graphics.setColor(Color.RED);
        graphics.drawString(text, x, y);

        return new Dimension(metrics.stringWidth(text), metrics.getHeight());
    }
}

