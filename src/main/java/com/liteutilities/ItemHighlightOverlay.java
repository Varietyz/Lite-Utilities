package com.liteutilities;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.ui.overlay.WidgetItemOverlay;
import net.runelite.client.game.ItemManager;

import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
public class ItemHighlightOverlay extends WidgetItemOverlay
{
	private final LiteUtilitiesPlugin plugin;
	private final ItemManager itemManager;

	@Inject
	private ItemHighlightOverlay(LiteUtilitiesPlugin plugin, ItemManager itemManager)
	{
		this.plugin = plugin;
		this.itemManager = itemManager;

		showOnEquipment();
		showOnInventory();
		showOnBank();
	}

	private long itemPrice(int itemId)
	{
		ItemComposition itemDef = itemManager.getItemComposition(itemId);
		long maxPrice = 0;
		int gePrice = itemManager.getItemPrice(itemId);
		int haPrice = itemDef.getHaPrice();

		if (plugin.getPriceType() == LiteUtilsPriceTypes.GRAND_EXCHANGE)
		{
			maxPrice = Math.max(maxPrice, gePrice);
		}
		else if (plugin.getPriceType() == LiteUtilsPriceTypes.HIGH_ALCHEMY)
		{
			maxPrice = Math.max(maxPrice, haPrice);
		}

		return maxPrice;
	}

	@Override
	public void renderItemOverlay(Graphics2D graphics, int itemId, WidgetItem itemWidget)
	{
		long price = itemPrice(itemId);
		price *= itemWidget.getQuantity();

		final Color color = plugin.getRarityColor(price);

		if (color == null || color.getAlpha() == 0)
		{
			return;
		}

		Rectangle bounds = itemWidget.getCanvasBounds();
		final BufferedImage outline = itemManager.getItemOutline(itemId, itemWidget.getQuantity(), color);
		graphics.drawImage(outline, (int)bounds.getX(), (int)bounds.getY(), null);
	}
}
