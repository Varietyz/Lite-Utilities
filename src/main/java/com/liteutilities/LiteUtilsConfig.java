package com.liteutilities;

import java.awt.Color;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;

@ConfigGroup(LiteUtilsConfig.GROUP)
public interface LiteUtilsConfig extends Config
{
	String GROUP = "inventorytotal";

	@ConfigSection(
		name = "Hotkeys",
		description = "Hotkey settings.",
		position = 1,
		closedByDefault = true
	)
	String HotKeySection = "HotKeySettings";

	@ConfigSection(
		name = "Advanced Settings",
		description = "Extra settings.",
		position = 2,
		closedByDefault = true
	)
	String AdvancedSection = "AdvancedSettings";

	@ConfigSection(
		name = "Container",
		description = "Container settings.",
		position = 3,
		closedByDefault = true
	)
	String ContainerSection = "ContainerSettings";

	@ConfigSection(
		position = 4,
		name = "Item Highlights",
		description = "Adjust the highlight color and value",
		closedByDefault = true
	)
	String highlightSection = "highlightSection";

	@ConfigItem(
			position = 0,
			keyName = "itemPricesSetting",
			name = "GP Source",
			description = "Configures the price unit between G.E and H.A."
	)
	default LiteUtilsPriceTypes priceType()
	{
		return LiteUtilsPriceTypes.GRAND_EXCHANGE;
	}

	@ConfigItem(
			position = 0,
			keyName = "enableProfitLossSetting",
			name = "Show Profit/Loss",
			description = "When enabled, displays profits on the counter."
	)
	default boolean enableProfitLoss()
	{
		return false;
	}

	@ConfigItem(
			position = 0,
			keyName = "showExactGpSetting",
			name = "Show True GP Value",
			description = "Configures whether or not the exact gp value is visible."
	)
	default boolean showExactGp()
	{
		return false;
	}

	@ConfigItem(
			keyName = "overlayEnabled",
			name = "Show Item Highlight",
			description = "Toggles whether the Item highlight is enabled or not. (Inventory & Bank)",
			position = 0
	)
	default boolean overlayEnabled() {
		return false;
	}

	@ConfigItem(
			position = 0,
			keyName = "profitLossToggleSetting",
			name = "Toggle Profit/Loss",
			description = "Switch between Total & Profit / Loss display.",
			section = HotKeySection
	)
	default Keybind profitLossToggleKey()
	{
		return new Keybind(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK);
	}

	@ConfigItem(
			position = 1,
			keyName = "resetProfitsSetting",
			name = "Reset Profits",
			description = "Resets the current profits and losses.",
			section = HotKeySection
	)
	default Keybind newRunKey()
	{
		return new Keybind(KeyEvent.VK_R, InputEvent.CTRL_DOWN_MASK);
	}

	@Alpha
	@ConfigItem(
			position = 1,
			keyName = "normalBackgroundColorSetting",
			name = "Background (Total)",
			description = "Configures the background color.",
			section = ContainerSection
	)
	default Color totalColor()
	{
		return new Color(17,17,17,225);
	}

	@Alpha
	@ConfigItem(
			position = 2,
			keyName = "profitBackgroundColorSetting",
			name = "Background (Profit)",
			description = "Configures profit background color.",
			section = ContainerSection
	)
	default Color profitColor()
	{
		return new Color(17,17,17,225);
	}

	@Alpha
	@ConfigItem(
			position = 4,
			keyName = "lossBackgroundColorSetting",
			name = "Background (Loss)",
			description = "Configures loss background color.",
			section = ContainerSection
	)
	default Color lossColor()
	{
		return new Color(17,17,17,225);
	}

	@ConfigItem(
			position = 0,
			keyName = "showContainerSetting",
			name = "Show Container",
			description = "Enable/Disable container.",
			section = ContainerSection
	)
	default boolean showContainer()
	{
		return false;
	}

	@ConfigItem(
			position = 5,
			keyName = "resetUponBankingSetting",
			name = "Auto-Reset Profits (Banking)",
			description = "Resets the profits/losses after banking.",
			section = AdvancedSection
	)
	default boolean newRunAfterBanking()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "lowValueColor",
		name = "Low Value",
		description = "Configures the color for low value items.",
		section = highlightSection,
		position = 1
	)
	default Color lowValueColor()
	{
		return new Color(101, 255, 141, 110);
	}

	@ConfigItem(
		keyName = "lowValuePrice",
		name = "",
		description = "Configures the start price for low value items.",
		section = highlightSection,
		position = 2
	)
	default int lowValuePrice()
	{
		return 10000;
	}

	@Alpha
	@ConfigItem(
		keyName = "mediumValueColor",
		name = "Medium Value",
		description = "Configures the color for medium value items.",
		section = highlightSection,
		position = 3
	)
	default Color mediumValueColor()
	{
		return new Color(0, 255, 250, 114);
	}

	@ConfigItem(
		keyName = "mediumValuePrice",
		name = "",
		description = "Configures the start price for medium value items.",
		section = highlightSection,
		position = 4
	)
	default int mediumValuePrice()
	{
		return 100000;
	}

	@Alpha
	@ConfigItem(
		keyName = "highValueColor",
		name = "High Value",
		description = "Configures the color for high value items.",
		section = highlightSection,
		position = 5
	)
	default Color highValueColor()
	{
		return new Color(255, 150, 0, 162);
	}

	@ConfigItem(
		keyName = "highValuePrice",
		name = "",
		description = "Configures the start price for high value items.",
		section = highlightSection,
		position = 6
	)
	default int highValuePrice()
	{
		return 1000000;
	}

	@Alpha
	@ConfigItem(
		keyName = "insaneValueColor",
		name = "Insane Value",
		description = "Configures the color for insane value items.",
		section = highlightSection,
		position = 7
	)
	default Color insaneValueColor()
	{
		return new Color(255, 0, 0, 167);
	}

	@ConfigItem(
		keyName = "insaneValuePrice",
		name = "",
		description = "Configures the start price for insane value items.",
		section = highlightSection,
		position = 8
	)
	default int insaneValuePrice()
	{
		return 10000000;
	}

	@ConfigItem(
			position = 5,
			keyName = "excludedItemsSetting",
			name = "Excluded Items",
			description = "Do not add these items to value calculation."
	)
	default String ignoredItems()
	{
		return "Cannon barrels, Cannon base, Cannon furnace, Cannon stand";
	}

}
