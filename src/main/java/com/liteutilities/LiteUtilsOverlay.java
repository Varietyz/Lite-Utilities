package com.liteutilities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

class LiteUtilsOverlay extends Overlay
{
	private static final int TEXT_Y_OFFSET = 17;
	private static final String PROFIT_LOSS_TIME_FORMAT = "%02d:%02d:%02d";
	private static final String PROFIT_LOSS_TIME_NO_HOURS_FORMAT = "%02d:%02d";
	private static final int HORIZONTAL_PADDING = 10;
	private static final int BANK_CLOSE_DELAY = 0;
	private static final Color TOOLTIP_BACKGROUND_COLOR = new Color(17, 17, 17, 225);

	private final Client client;
	private final LiteUtilitiesPlugin plugin;
	private final LiteUtilsConfig config;

	private final ItemManager itemManager;

	private Widget inventoryWidget;
	@Getter
	private ItemContainer inventoryItemContainer;
	@Getter
	private ItemContainer equipmentItemContainer;

	private boolean onceBank = false;

	private boolean showInterstitial = false;

	private boolean postNewRun = false;
	private long newRunTime = 0;

	private int invX = -1;
	private int invY = -1;
	private int invW = -1;

	private int canvasX = 0;
	private int canvasY = 0;
	private int canvasWidth = 0;
	private int canvasHeight = 0;

	@Inject
	private LiteUtilsOverlay(Client client, LiteUtilitiesPlugin plugin, LiteUtilsConfig config, ItemManager itemManager)
	{
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ALWAYS_ON_TOP);

		this.client = client;
		this.plugin = plugin;
		this.config = config;

		this.itemManager = itemManager;
	}

	private Widget getViewportWidget()
	{
		Widget widget;

		widget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_INTERFACE_CONTAINER);
		if (widget != null) return widget;

		widget = client.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_INTERFACE_CONTAINER);
		if (widget != null) return widget;

		widget = client.getWidget(ComponentID.FIXED_VIEWPORT_INTERFACE_CONTAINER);
		if (widget != null) return widget;

		return client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER);
	}

	void updatePluginState()
	{
		inventoryWidget = client.getWidget(ComponentID.INVENTORY_CONTAINER);
		inventoryItemContainer = client.getItemContainer(InventoryID.INVENTORY);
		equipmentItemContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (plugin.getPLToggleOverride() == null)
		{
			if (config.enableProfitLoss())
			{
				plugin.setMode(LiteUtilsModes.PROFIT_LOSS);
			}
			else
			{
				plugin.setMode(LiteUtilsModes.TOTAL);
			}
		}
		else if (plugin.getPLToggleOverride() == LiteUtilsModes.PROFIT_LOSS)
		{
			plugin.setMode(LiteUtilsModes.PROFIT_LOSS);
		}
		else if (plugin.getPLToggleOverride() == LiteUtilsModes.TOTAL)
		{
			plugin.setMode(LiteUtilsModes.TOTAL);
		}

		boolean isBank = false;

		if (inventoryWidget == null || inventoryWidget.getCanvasLocation().getX() < 0 || inventoryWidget.isHidden())
		{
			Widget[] altInventoryWidgets = new Widget[]
				{
				client.getWidget(ComponentID.BANK_INVENTORY_ITEM_CONTAINER),
				client.getWidget(ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER)
			};

			for (Widget altInventoryWidget : altInventoryWidgets)
			{
				inventoryWidget = altInventoryWidget;
				if (inventoryWidget != null && !inventoryWidget.isHidden())
				{
					isBank = true;
					if (!onceBank)
					{
						onceBank = true;
					}
					break;
				}
			}
		}

		if (isBank)
		{
			plugin.setState(LiteUtilsState.BANK);
		}
		else
		{
			plugin.setState(LiteUtilsState.RUN);
		}

		boolean newRun = (plugin.getPreviousState() == LiteUtilsState.BANK
			&& plugin.getState() == LiteUtilsState.RUN
			&& config.newRunAfterBanking()) || plugin.isManualNewRun();
		plugin.getRunData().itemQtys.clear();

		long[] inventoryTotals = plugin.getInventoryTotals(false);
		long[] equipmentTotals = plugin.getEquipmentTotals(false);

		if (inventoryTotals.length >= LiteUtilitiesPlugin.TOTAL_QTY_INDEX + 1)
		{
			long inventoryTotal = inventoryTotals[LiteUtilitiesPlugin.TOTAL_GP_GE_INDEX];
			long inventoryTotalHA = inventoryTotals[LiteUtilitiesPlugin.TOTAL_GP_HA_INDEX];
			long inventoryQty = inventoryTotals[LiteUtilitiesPlugin.TOTAL_QTY_INDEX];

			long totalGp = 0;
			if (config.priceType() == LiteUtilsPriceTypes.GRAND_EXCHANGE)
			{
				totalGp += inventoryTotal;
			}
			else
			{
				totalGp += inventoryTotalHA;
			}

			if (equipmentTotals.length >= 2)
			{
				long equipmentTotal = equipmentTotals[0];
				long equipmentTotalHA = equipmentTotals[1];

				if ((plugin.getState() == LiteUtilsState.RUN || !config.newRunAfterBanking())
					&& plugin.getMode() == LiteUtilsModes.PROFIT_LOSS)
				{
					if (config.priceType() == LiteUtilsPriceTypes.GRAND_EXCHANGE)
					{
						totalGp += equipmentTotal;
					}
					else
					{
						totalGp += equipmentTotalHA;
					}
				}
			}

			plugin.setTotalGp(totalGp);
			plugin.setTotalQty(inventoryQty);
		}
		else
		{
			plugin.setTotalGp(0);
			plugin.setTotalQty(0);
		}

		if (newRun)
		{
			plugin.onNewRun();
			postNewRun = true;
			newRunTime = Instant.now().toEpochMilli();
		}
		else if (plugin.getPreviousState() == LiteUtilsState.RUN && plugin.getState() == LiteUtilsState.BANK)
		{
			plugin.onBank();
		}

		if (postNewRun && (Instant.now().toEpochMilli() - newRunTime) > BANK_CLOSE_DELAY)
		{
			plugin.postNewRun();
			postNewRun = false;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		updatePluginState();

		Widget viewportWidget = getViewportWidget();
		Widget depositBox = client.getWidget(ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER);

		if (viewportWidget == null || (depositBox != null && depositBox.isHidden()))
		{
			return null;
		}

		net.runelite.api.Point viewportCanvasLocation = viewportWidget.getCanvasLocation();
		if (viewportCanvasLocation == null)
		{
			return null;
		}

		canvasX = viewportCanvasLocation.getX();
		canvasY = viewportCanvasLocation.getY();
		canvasWidth = viewportWidget.getWidth() + 28;
		canvasHeight = viewportWidget.getHeight() + 41;

		int invH;
		if (inventoryWidget != null)
		{
			net.runelite.api.Point invCanvasLocation = inventoryWidget.getCanvasLocation();
			if (invCanvasLocation != null)
			{
				invX = invCanvasLocation.getX();
				invY = invCanvasLocation.getY();
				invW = inventoryWidget.getWidth();
				invH = inventoryWidget.getHeight();
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}

		if (invX < 0 || invY < 0 || invW < 0 || invH < 0)
		{
			return null;
		}

		if (inventoryWidget.isHidden())
		{
			return null;
		}

		int height = 20;

		if (plugin == null)
		{
			return null;
		}

		long total = plugin.getProfitGp();
		String totalText = getTotalText(total);


		if (showInterstitial)
		{
			total = 0;
			if (plugin.getMode() == LiteUtilsModes.PROFIT_LOSS)
			{
				totalText = "0";
			}
			else
			{
				totalText = getTotalText(plugin.getProfitGp());
			}
		}

		renderTotal(config, graphics, plugin,
			plugin.getTotalQty(), total, totalText, height);

		return null;
	}

	private void renderTotal(LiteUtilsConfig config, Graphics2D graphics, LiteUtilitiesPlugin plugin,
							 long totalQty, long total, String totalText,
							 int height)
	{

		if (plugin.getMode() == LiteUtilsModes.PROFIT_LOSS)
		{
			long profitGp = plugin.getProfitGp();
			totalText = profitGp > 0 ? ('+' + totalText) : totalText;
		}

		int imageSize = 15;
		int numCoins;
		if (total > Integer.MAX_VALUE)
		{
			numCoins = Integer.MAX_VALUE;
		}
		else if (total < Integer.MIN_VALUE)
		{
			numCoins = Integer.MIN_VALUE;
		}
		else
		{
			numCoins = (int) total;
			if (numCoins == 0)
			{
				numCoins = 1000000;
			}
		}
		numCoins = Math.abs(numCoins);

		if (totalQty == 0)
		{
			return;
		}

		graphics.setFont(FontManager.getRunescapeSmallFont());
		final int totalWidth = graphics.getFontMetrics().stringWidth(totalText);

		int fixedRunTimeWidth = 0;
		int actualRunTimeWidth = 0;
		int imageWidthWithPadding;

		imageWidthWithPadding = imageSize + 3;

		int width = totalWidth + fixedRunTimeWidth + imageWidthWithPadding + HORIZONTAL_PADDING * 2;

		int x = invX + invW - width;

		int xOffset = 26;

		x += xOffset;

		int yOffset = 36;

		int y = invY - height - yOffset;

		Color backgroundColor;
		Color borderColor;
		Color textColor;

		if (numCoins >= 1_000_000_000)
		{
			textColor = new Color(0x6698FF);
		}
		else if (numCoins >= 10_000_000)
		{
			textColor = new Color(0x00FF80);
		}
		else if (numCoins >= 100_000)
		{
			textColor = new Color(0xFFFFFF);
		}
		else if (numCoins > 0)
		{
			textColor = new Color(0xFFFF00);
		}
		else
		{
			textColor = new Color(0xFF0000);
		}

		if ((plugin.getState() == LiteUtilsState.BANK && config.newRunAfterBanking())
			|| plugin.getMode() == LiteUtilsModes.TOTAL)
		{
			backgroundColor = config.totalColor();
			borderColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), 125);
		}
		else if (total > 0)
		{
			backgroundColor = config.profitColor();
			borderColor = new Color(0,255,0,125);
			textColor = new Color(0,255,0,255);
		}
		else
		{
			backgroundColor = config.lossColor();
			borderColor = new Color(255,0,0,125);
			textColor = new Color(255,0,0,255);
		}

		int cornerRadius = 0;

		int containerAlpha = backgroundColor.getAlpha();

		if (config.showContainer())
		{
			if (containerAlpha > 0)
			{
				graphics.setColor(borderColor);
				graphics.drawRoundRect(x + 1, y, width - 7, height - 2, cornerRadius, cornerRadius);

				graphics.setColor(backgroundColor);

				graphics.fillRoundRect(x + 2, y + 1, width - 8, height - 3, cornerRadius, cornerRadius);
			}
		}

		TextComponent textComponent = new TextComponent();

		textComponent.setColor(textColor);
		textComponent.setText(totalText);
		textComponent.setPosition(new Point(x + HORIZONTAL_PADDING, y + TEXT_Y_OFFSET - 1));
		textComponent.render(graphics);

		int imageOffset = 4;
		BufferedImage coinsImage = itemManager.getImage(ItemID.COINS_995, numCoins, false);

		BufferedImage resizedImage = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resizedImage.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.drawImage(coinsImage, 0, 0, imageSize, imageSize, null);
		g2d.dispose();

		int coinImageX = (x + width) - HORIZONTAL_PADDING - imageSize + imageOffset;
		int coinImageY = y + 2;

		graphics.drawImage(resizedImage, coinImageX, coinImageY, null);

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		Rectangle coinImageBounds = new Rectangle(coinImageX, coinImageY, imageSize, imageSize);

		if (coinImageBounds.contains(mouseX, mouseY) &&
			(plugin.getState() != LiteUtilsState.BANK || !config.newRunAfterBanking()) &&
			(Instant.now().toEpochMilli() - newRunTime) > (BANK_CLOSE_DELAY))
		{
			renderUnifiedLedger(graphics);
		}
	}

	private BufferedImage getScaledItemImage(long itemId)
	{
		BufferedImage itemImage;

		if (itemId == ItemID.COINS_995)
		{
			int xCoins = 0;

			if (inventoryItemContainer != null)
			{
				for (Item item : inventoryItemContainer.getItems())
				{
					if (item.getId() == ItemID.COINS_995)
					{
						xCoins += item.getQuantity();
					}
				}
			}
			itemImage = itemManager.getImage(ItemID.COINS_995, Math.abs(xCoins), false);
		}
		else
		{
			itemImage = itemManager.getImage((int) itemId);
		}

		if (itemImage == null)
		{
			return null;
		}

		int targetWidth = 12;

		BufferedImage resizedImage = new BufferedImage(targetWidth, 12, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d = resizedImage.createGraphics();

		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		g2d.drawImage(itemImage, 0, 0, targetWidth, 12, null);
		g2d.dispose();

		return resizedImage;
	}

	private String formatPrice(long value)
	{
		if (config.showExactGp())
		{
			return NumberFormat.getInstance(Locale.ENGLISH).format(value);
		}
		else
		{
			return getFormattedGp(value);
		}
	}

	private void renderUnifiedLedger(Graphics2D graphics)
	{
		FontMetrics fontMetrics = graphics.getFontMetrics();

		List<LiteUtilsTooltipItem> inventoryLedger = plugin.getInventoryLedger().stream()
			.filter(item -> item.getQty() != 0)
			.collect(Collectors.toList());

		List<LiteUtilsTooltipItem> profitLossLedger = plugin.getProfitLossLedger().stream()
			.filter(item -> item.getQty() != 0)
			.collect(Collectors.toList());

		List<LiteUtilsTooltipItem> gainsLedger = profitLossLedger.stream()
			.filter(item -> item.getQty() > 0)
			.collect(Collectors.toList());

		List<LiteUtilsTooltipItem> lossesLedger = profitLossLedger.stream()
			.filter(item -> item.getQty() < 0)
			.collect(Collectors.toList());


		List<Long> inventoryItemIds = inventoryLedger.stream()
			.map(LiteUtilsTooltipItem::getItemId)
			.filter(id -> id != -1)
			.distinct()
			.collect(Collectors.toList());

		List<Long> profitLossItemIds = profitLossLedger.stream()
			.map(LiteUtilsTooltipItem::getItemId)
			.filter(id -> id != -1)
			.distinct()
			.collect(Collectors.toList());

		List<Long> allItemIds = Stream.concat(inventoryItemIds.stream(), profitLossItemIds.stream())
			.distinct()
			.collect(Collectors.toList()); // FOR FUTURE REFERENCING

		if (inventoryLedger.isEmpty() && profitLossLedger.isEmpty())
		{
			return;
		}

		inventoryLedger = inventoryLedger.stream()
			.sorted(Comparator.comparingLong(o -> -(o.getQty() * o.getAmount())))
			.collect(Collectors.toList());

		java.util.List<LiteUtilsTooltipItem> gain = profitLossLedger.stream()
			.filter(item -> item.getQty() > 0)
			.sorted(Comparator.comparingLong(o -> -(o.getQty() * o.getAmount())))
			.collect(Collectors.toList());

		java.util.List<LiteUtilsTooltipItem> loss = profitLossLedger.stream()
			.filter(item -> item.getQty() < 0)
			.sorted(Comparator.comparingLong(o -> (o.getQty() * o.getAmount())))
			.collect(Collectors.toList());

		long totalInventory = inventoryLedger.stream().mapToLong(item -> item.getQty() * item.getAmount()).sum();
		long totalGain = gain.stream().mapToLong(item -> item.getQty() * item.getAmount()).sum();
		long totalLoss = loss.stream().mapToLong(item -> item.getQty() * item.getAmount()).sum();
		long totalProfitLoss = totalGain + totalLoss;

		inventoryLedger.add(new LiteUtilsTooltipItem("Total Worth:", 1, totalInventory, 0));
		gainsLedger.add(new LiteUtilsTooltipItem("Gained:", 1, totalGain, 0));
		lossesLedger.add(new LiteUtilsTooltipItem("Lost:", 1, totalLoss, 0));

		String[] inventoryDescriptions = inventoryLedger.stream()
			.map(this::formatDescription)
			.toArray(String[]::new);

		Long[] inventoryPrices = inventoryLedger.stream()
			.map(item -> item.getQty() * item.getAmount())
			.toArray(Long[]::new);

		String[] profitLossDescriptions = Stream.concat(
			gainsLedger.stream().map(this::formatDescription),
			lossesLedger.stream().map(this::formatDescription)
		).toArray(String[]::new);

		String[] descriptions = Stream.concat(Arrays.stream(inventoryDescriptions), Arrays.stream(profitLossDescriptions))
			.toArray(String[]::new);

		Long[] prices = Stream.concat(Arrays.stream(inventoryPrices),
			Stream.concat(
				gainsLedger.stream().map(item -> item.getQty() * item.getAmount()),
				lossesLedger.stream().map(item -> item.getQty() * item.getAmount())
			)
		).toArray(Long[]::new);

		String[] formattedPrices = new String[prices.length];
		for (int i = 0; i < prices.length; i++)
		{
			long price = prices[i];
			if (i < inventoryLedger.size())
			{
				formattedPrices[i] = formatPrice(Math.abs(price));
			}
			else
			{
				if (price > 0)
				{
					formattedPrices[i] = "+" + formatPrice(price);
				}
				else if (price < 0)
				{
					formattedPrices[i] = formatPrice(price);
				}
				else
				{
					formattedPrices[i] = "0";
				}
			}
		}

		int maxRowWidth = 0;
		for (int i = 0; i < descriptions.length; i++)
		{
			int rowWidth = fontMetrics.stringWidth(descriptions[i]) + fontMetrics.stringWidth(formattedPrices[i]) + HORIZONTAL_PADDING * 2;
			maxRowWidth = Math.max(maxRowWidth, rowWidth);
		}

		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int mouseX = mouse.getX();
		int mouseY = mouse.getY();

		int sectionPadding = 5;
		int rowH = fontMetrics.getHeight();

		int totalHeight = (descriptions.length * rowH) + TEXT_Y_OFFSET + sectionPadding + 50;

		int x = mouseX - maxRowWidth - 10;
		int y = mouseY - totalHeight / 2;

		x = Math.max(5, Math.min(x, canvasX + canvasWidth - maxRowWidth - 5));
		y = Math.max(5, Math.min(y, canvasY + canvasHeight - totalHeight - 10));

		Color colorWithAlpha;

		if (totalProfitLoss > 0)
		{
			colorWithAlpha = new Color(0, 255, 0, 75);
		}
		else if (totalProfitLoss < 0)
		{
			colorWithAlpha = new Color(255, 0, 0, 75);
		}
		else
		{
			colorWithAlpha = new Color(255, 255, 0, 75);
		}

		graphics.setColor(TOOLTIP_BACKGROUND_COLOR);
		graphics.fillRoundRect(x, y, maxRowWidth + 12, totalHeight, 0, 0);
		graphics.setColor(colorWithAlpha);
		graphics.setStroke(new BasicStroke(1));
		graphics.drawRoundRect(x, y, maxRowWidth + 12, totalHeight, 0, 0);

		int yOffset = 0;

		for (int i = 0; i < inventoryLedger.size(); i++)
		{
			String desc = inventoryDescriptions[i];
			long price = inventoryPrices[i];

			if (desc.equals("Total Worth:"))
			{
				int textX = x + HORIZONTAL_PADDING / 2 + 2;
				int textY = y + rowH * i + TEXT_Y_OFFSET + 5 + yOffset;

				TextComponent textComponent = new TextComponent();
				textComponent.setColor(getTextColor(desc));
				textComponent.setText(desc);
				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);

				String formattedPrice = formattedPrices[i];
				int textW = fontMetrics.stringWidth(formattedPrice);
				textX = x + maxRowWidth + 12 - HORIZONTAL_PADDING / 2 - textW;

				textComponent.setText(formattedPrice);
				textComponent.setColor(getPriceColor(price));
				textComponent.setPosition(new Point(textX, textY));
				textComponent.render(graphics);

				continue;
			}

			long itemId = inventoryLedger.get(i).getItemId();

			BufferedImage itemImage = getScaledItemImage(itemId);

			assert itemImage != null;
			int imageWidth = itemImage.getWidth();
			int textX = x + HORIZONTAL_PADDING / 2 + imageWidth + 2;
			int textY = y + rowH * i + TEXT_Y_OFFSET + yOffset;

			int imageX = x + HORIZONTAL_PADDING / 2;
			int imageY = textY - fontMetrics.getAscent() + 1;

			graphics.drawImage(itemImage, imageX, imageY - 2, null);

			TextComponent textComponent = new TextComponent();
			textComponent.setColor(getTextColor(desc));
			textComponent.setText(desc);
			textComponent.setPosition(new Point(textX, textY));
			textComponent.render(graphics);

			String formattedPrice = formattedPrices[i];
			int textW = fontMetrics.stringWidth(formattedPrice);
			textX = x + maxRowWidth + imageWidth - HORIZONTAL_PADDING / 2 - textW;

			textComponent.setText(formattedPrice);
			textComponent.setColor(getPriceColor(price));
			textComponent.setPosition(new Point(textX, textY));
			textComponent.render(graphics);
		}

		int dashedLineY = y + (inventoryLedger.size() * rowH) + TEXT_Y_OFFSET + yOffset;

		float[] dashPattern = {5f, 5f};
		BasicStroke dashedStroke = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dashPattern, 0f);
		graphics.setStroke(dashedStroke);
		graphics.setColor(colorWithAlpha);

		graphics.drawLine(x + 10, dashedLineY, x + maxRowWidth + 2, dashedLineY);

		int currentY = dashedLineY + rowH + sectionPadding;
		for (LiteUtilsTooltipItem liteUtilsTooltipItem : gainsLedger)
		{
			String desc = formatDescription(liteUtilsTooltipItem);
			long price = liteUtilsTooltipItem.getQty() * liteUtilsTooltipItem.getAmount();

			if (desc.equals("Gained:"))
			{
				currentY += rowH + sectionPadding - 11;
			}

			Color gainColor = Color.GREEN;

			BufferedImage itemImage = null;
			if (!desc.equals("Gained:"))
			{
				long itemId = liteUtilsTooltipItem.getItemId();
				itemImage = getScaledItemImage(itemId);

				if (itemImage != null)
				{
					int imageX = x + HORIZONTAL_PADDING / 2;
					int imageY = currentY - fontMetrics.getAscent() + 1;
					graphics.drawImage(itemImage, imageX, imageY - 2, null);
				}
			}

			int textX = x + HORIZONTAL_PADDING / 2 + (itemImage != null ? itemImage.getWidth() + 2 : 0) + (desc.equals("Gained:") ? 2 : 0);
			int textY = currentY;

			TextComponent textComponent = new TextComponent();
			textComponent.setColor(gainColor);
			textComponent.setText(desc);
			textComponent.setPosition(new Point(textX, textY));
			textComponent.render(graphics);

			String formattedPrice = "+" + formatPrice(price);
			int priceTextX = x + maxRowWidth + 12 - HORIZONTAL_PADDING / 2 - fontMetrics.stringWidth(formattedPrice);
			textComponent.setText(formattedPrice);
			textComponent.setColor(gainColor);
			textComponent.setPosition(new Point(priceTextX, textY));
			textComponent.render(graphics);

			currentY += rowH;
		}

		currentY += sectionPadding - 13;
		graphics.setStroke(dashedStroke);
		graphics.setColor(colorWithAlpha);
		graphics.drawLine(x + 10, currentY, x + maxRowWidth + 2, currentY);

		currentY += rowH + sectionPadding;
		for (LiteUtilsTooltipItem liteUtilsTooltipItem : lossesLedger)
		{
			String desc = formatDescription(liteUtilsTooltipItem);
			long price = liteUtilsTooltipItem.getQty() * liteUtilsTooltipItem.getAmount();

			if (desc.equals("Lost:"))
			{
				currentY += rowH + sectionPadding - 11;
			}

			Color lossColor = Color.RED;

			BufferedImage itemImage = null;
			if (!desc.equals("Lost:"))
			{
				long itemId = liteUtilsTooltipItem.getItemId();
				itemImage = getScaledItemImage(itemId);

				if (itemImage != null)
				{
					int imageX = x + HORIZONTAL_PADDING / 2;
					int imageY = currentY - fontMetrics.getAscent() + 1;
					graphics.drawImage(itemImage, imageX, imageY - 2, null);
				}
			}

			int textX = x + HORIZONTAL_PADDING / 2 + (itemImage != null ? itemImage.getWidth() + 2 : 0) + (desc.equals("Lost:") ? 2 : 0);
			int textY = currentY;

			TextComponent textComponent = new TextComponent();
			textComponent.setColor(lossColor);
			textComponent.setText(desc);
			textComponent.setPosition(new Point(textX, textY));
			textComponent.render(graphics);

			String formattedPrice = formatPrice(price);
			int priceTextX = x + maxRowWidth + 12 - HORIZONTAL_PADDING / 2 - fontMetrics.stringWidth(formattedPrice);
			textComponent.setText(formattedPrice);
			textComponent.setColor(lossColor);
			textComponent.setPosition(new Point(priceTextX, textY));
			textComponent.render(graphics);

			currentY += rowH;
		}

		currentY += sectionPadding - 13;
		graphics.setStroke(dashedStroke);
		graphics.setColor(colorWithAlpha);
		graphics.drawLine(x + 10, currentY, x + maxRowWidth + 2, currentY);

		currentY += sectionPadding + 13;

		String profitDesc;
		if (totalProfitLoss > 0)
		{
			profitDesc = "Earning:";
		}
		else if (totalProfitLoss < 0)
		{
			profitDesc = "Losing:";
		}
		else
		{
			profitDesc = "Break-even:";
		}

		String formattedProfit = (totalProfitLoss > 0 ? "+" : "") + formatPrice(totalProfitLoss);
		Color profitColor;
		if (totalProfitLoss > 0)
		{
			profitColor = Color.GREEN;
		}
		else if (totalProfitLoss < 0)
		{
			profitColor = Color.RED;
		}
		else
		{
			profitColor = Color.YELLOW;
		}

		TextComponent textComponent = new TextComponent();
		textComponent.setColor(profitColor);
		textComponent.setText(profitDesc);
		textComponent.setPosition(new Point(x + HORIZONTAL_PADDING / 2 + 2, currentY));
		textComponent.render(graphics);

		int profitTextX = x + maxRowWidth + 12 - HORIZONTAL_PADDING / 2 - fontMetrics.stringWidth(formattedProfit);
		textComponent.setText(formattedProfit);
		textComponent.setColor(profitColor);
		textComponent.setPosition(new Point(profitTextX, currentY));
		textComponent.render(graphics);
	}

	private String formatDescription(LiteUtilsTooltipItem item)
	{
		String desc = item.getDescription();
		if (item.getQty() != 0 && Math.abs(item.getQty()) != 1
			&& !item.getDescription().contains("Total") && !item.getDescription().contains("Coins"))
		{
			desc = NumberFormat.getInstance(Locale.ENGLISH).format(Math.abs(item.getQty())) + " " + desc;
		}
		return desc;
	}

	private Color getTextColor(String description)
	{
		if (description.contains("Total"))
		{
			return description.contains("Profit/Loss") ? Color.YELLOW : Color.ORANGE;
		}
		return Color.decode("#FFF7E3");
	}

	private Color getPriceColor(long price)
	{
		long numCoins = Math.abs(price);
		if (numCoins >= 1_000_000_000)
		{
			return new Color(0x6698FF);
		}
		else if (numCoins >= 10_000_000)
		{
			return new Color(0x00FF80);
		}
		else if (numCoins >= 100_000)
		{
			return new Color(0xFFFFFF);
		}
		else if (numCoins > 0)
		{
			return new Color(0xFFFF00);
		}
		else
		{
			return Color.RED;
		}
	}

	private String getTotalText(long total)
	{
		if (config.showExactGp())
		{
			return getExactFormattedGp(total);
		}
		else
		{
			String totalText = getFormattedGp(total);
			return totalText.replace(".0", "");
		}
	}

	private String getFormattedGp(long total)
	{
		if (total >= 1000000000 || total <= -1000000000)
		{
			double bTotal = total / 1000000000.0;
			return getTruncatedTotal(bTotal) + "B";
		}
		else
		{
			if (total >= 1000000 || total <= -1000000)
			{
				double mTotal = total / 1000000.0;
				return getTruncatedTotal(mTotal) + "M";
			}
			else
			{
				if (total >= 1000 || total <= -1000)
				{
					double kTotal = total / 1000.0;
					return getTruncatedTotal(kTotal) + "K";
				}
				else
				{
					return getExactFormattedGp(total);
				}
			}
		}
	}

	private String getTruncatedTotal(double total)
	{
		String totalString = Double.toString(total);

		long dotIndex = totalString.indexOf('.');
		if (dotIndex < totalString.length() - 1)
		{
			return totalString.substring(0, Math.toIntExact(dotIndex + 2));
		}

		return totalString;
	}

	private String getExactFormattedGp(long total)
	{
		return NumberFormat.getInstance(Locale.ENGLISH).format(total);
	}

	public void showInterstitial()
	{
		showInterstitial = true;
	}

	public void hideInterstitial()
	{
		showInterstitial = false;
	}
}
