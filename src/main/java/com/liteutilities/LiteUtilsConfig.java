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
