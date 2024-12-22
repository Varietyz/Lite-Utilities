package com.liteutilities;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.ItemID;
import net.runelite.api.Varbits;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.RuneScapeProfileChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

@PluginDescriptor(
	name = "Lite Utilities",
	description = "Utilities that are compact yet informative, display profits and inventory value.",
	tags = {"combat", "profit", "gold", "items", "inventory", "tracking", "calculate", "skilling", "money", "pouch", "highlight"},
	conflicts = {"Inventory Total", "ItemRarity"}
)

public class LiteUtilitiesPlugin extends Plugin
{
	static final int COINS = ItemID.COINS_995;
	static final int TOTAL_GP_GE_INDEX = 0;
	static final int TOTAL_GP_HA_INDEX = 1;
	static final int TOTAL_QTY_INDEX = 2;
	static final int RUNEPOUCH_ITEM_ID = 12791;
	static final int DIVINE_RUNEPOUCH_ITEM_ID = 27281;

	@Inject
	private LiteUtilsOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ItemHighlightOverlay overlayItem;

	@Inject
	private Client client;

	@Inject
	private LiteUtilsConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Gson gson;

	@Inject
	private KeyManager keyManager;

	@Getter
	private LiteUtilsTimerData runData;

	@Getter
	private LiteUtilsModes mode = LiteUtilsModes.TOTAL;

	@Getter
	private LiteUtilsState state = LiteUtilsState.NONE;
	private LiteUtilsState prevState = LiteUtilsState.NONE;

	private long totalGp = 0;
	@Getter
	private long totalQty = 0;

	private long runStartTime = 0;

	private long lastWriteSaveTime = 0;

	private LiteUtilsModes plToggleOverride = null;
	private KeyListener plToggleKeyListener;
	private KeyListener newRunKeyListener;

	private boolean manualNewRun = false;

	private static final long[] RUNEPOUCH_AMOUNT_VARBITS = {
			Varbits.RUNE_POUCH_AMOUNT1, Varbits.RUNE_POUCH_AMOUNT2, Varbits.RUNE_POUCH_AMOUNT3, Varbits.RUNE_POUCH_AMOUNT4
	};
	private static final long[] RUNEPOUCH_RUNE_VARBITS = {
			Varbits.RUNE_POUCH_RUNE1, Varbits.RUNE_POUCH_RUNE2, Varbits.RUNE_POUCH_RUNE3, Varbits.RUNE_POUCH_RUNE4
	};

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		if (config.overlayEnabled()) {
			overlayManager.add(overlayItem);
		}

		runData = new LiteUtilsTimerData();
		manualNewRun = true;
		registerKeys();
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
		overlayManager.remove(overlayItem);
		unregisterKeys();
	}

	@Subscribe
	public void onRuneScapeProfileChanged(RuneScapeProfileChanged e)
	{
		String profileKey = configManager.getRSProfileKey();
		if (profileKey != null)
		{
			runData = getSavedData();
		}
	}

	@Provides
	LiteUtilsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(LiteUtilsConfig.class);
	}

	LiteUtilsPriceTypes getPriceType()
	{
		return config.priceType();
	}

	Color getRarityColor(final long itemPrice)
	{
		if (itemPrice >= config.insaneValuePrice())
		{
			return config.insaneValueColor();
		}
		else if (itemPrice >= config.highValuePrice())
		{
			return config.highValueColor();
		}
		else if (itemPrice >= config.mediumValuePrice())
		{
			return config.mediumValueColor();
		}
		else if (itemPrice >= config.lowValuePrice())
		{
			return config.lowValueColor();
		}

		return null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged config)
	{
		if (config.getGroup().equals(LiteUtilsConfig.GROUP))
		{
			if (config.getKey().equals("overlayEnabled"))
			{
				boolean overlayEnabled = configManager.getConfig(LiteUtilsConfig.class).overlayEnabled();

				if (overlayEnabled)
				{
					overlayManager.add(overlayItem);
				}
				else
				{
					overlayManager.remove(overlayItem);
				}
			}
			else if (config.getKey().equals("enableProfitLoss"))
			{
				plToggleOverride = null;
			}
			else if (config.getKey().equals("profitLossToggleKey"))
			{
				unregisterKeys();
				registerKeys();
			}
			else if (config.getKey().equals("ignoredItems"))
			{
				if (runData != null)
				{
					runData.ignoredItems = getIgnoredItems();
				}
			}
		}
	}

	private void registerKeys()
	{
		plToggleKeyListener = new HotkeyListener(() -> config.profitLossToggleKey())
		{
			@Override
			public void hotkeyPressed()
			{
				if (mode == LiteUtilsModes.TOTAL)
				{
					plToggleOverride = LiteUtilsModes.PROFIT_LOSS;
				}
				else
				{
					plToggleOverride = LiteUtilsModes.TOTAL;
				}
			}
		};
		keyManager.registerKeyListener(plToggleKeyListener);

		newRunKeyListener = new HotkeyListener(() -> config.newRunKey())
		{
			@Override
			public void hotkeyPressed()
			{
				if (state != LiteUtilsState.BANK)
				{
					manualNewRun = true;
				}
			}
		};
		keyManager.registerKeyListener(newRunKeyListener);
	}

	private void unregisterKeys()
	{
		if (plToggleKeyListener != null)
		{
			keyManager.unregisterKeyListener(plToggleKeyListener);
		}
		if (newRunKeyListener != null)
		{
			keyManager.unregisterKeyListener(newRunKeyListener);
		}
	}

	void onNewRun()
	{
		overlay.showInterstitial();

		runStartTime = Instant.now().toEpochMilli();

		runData.ignoredItems = getIgnoredItems();
	}

	void postNewRun()
	{
		runData.initialItemQtys.clear();

		long [] inventoryTotals = getInventoryTotals(true);
		long [] equipmentTotals = getEquipmentTotals(true);

		long inventoryTotal = inventoryTotals[LiteUtilitiesPlugin.TOTAL_GP_GE_INDEX];
		long inventoryTotalHA = inventoryTotals[LiteUtilitiesPlugin.TOTAL_GP_HA_INDEX];

		long equipmentTotal = equipmentTotals[0];
		long equipmentTotalHA = equipmentTotals[1];

		runData.profitLossInitialGp = inventoryTotal + equipmentTotal;
		runData.profitLossInitialGpHA = inventoryTotalHA + equipmentTotalHA;

		writeSavedData();

		overlay.hideInterstitial();
	}

	void onBank()
	{
		if (!config.newRunAfterBanking())
		{
			return;
		}

		runData.profitLossInitialGp = 0;
		runData.profitLossInitialGpHA = 0;
		runData.itemPrices.clear();

		runStartTime = 0;

		writeSavedData();
	}

	long[] getInventoryTotals(boolean isNewRun)
	{
		final ItemContainer itemContainer = overlay.getInventoryItemContainer();

		if (itemContainer == null)
		{
			return new long [2];
		}

		final Item[] items = itemContainer.getItems();

		final LinkedList<Item> allItems = new LinkedList<>(Arrays.asList(items));
		if (allItems.stream().anyMatch(s -> s.getId() == RUNEPOUCH_ITEM_ID || s.getId() == DIVINE_RUNEPOUCH_ITEM_ID))
		{
			allItems.addAll(getRunepouchContents());
		}

		long totalQty = 0;
		long totalGp = 0;
		long totalGpHA = 0;

		for (Item item: allItems)
		{
			long itemId = item.getId();

			final ItemComposition itemComposition = itemManager.getItemComposition(Math.toIntExact(itemId));

			String itemName = itemComposition.getName();
			final boolean ignore = runData.ignoredItems.stream().anyMatch(s -> {
				String lcItemName = itemName.toLowerCase();
				String lcS = s.toLowerCase();
				return lcItemName.contains(lcS);
			});
			if (ignore)
			{
				continue;
			}

			final boolean isNoted = itemComposition.getNote() != -1;
			final long realItemId = isNoted ? itemComposition.getLinkedNoteId() : itemId;

			long totalPrice;
			long totalPriceHA;
			long gePrice;
			long haPrice;

			if (runData.itemPrices.containsKey(realItemId))
			{
				gePrice = runData.itemPrices.get(realItemId);
			}
			else
			{
				gePrice = itemManager.getItemPrice(Math.toIntExact(realItemId));
			}

			if (runData.itemPricesHA.containsKey(realItemId))
			{
				haPrice = runData.itemPricesHA.get(realItemId);
			}
			else
			{
				haPrice = itemComposition.getHaPrice();
			}

			long itemQty = item.getQuantity();

			if (realItemId == COINS)
			{
				totalPrice = itemQty;
			}
			else
			{
				totalPrice = itemQty * gePrice;
			}

			if (realItemId == COINS)
			{
				totalPriceHA = itemQty;
			}
			else
			{
				totalPriceHA = itemQty * haPrice;
			}

			totalGp += totalPrice;
			totalGpHA += totalPriceHA;
			totalQty += itemQty;

			if (realItemId != COINS && !runData.itemPrices.containsKey(realItemId))
			{
				runData.itemPrices.put(realItemId, gePrice);
			}

			if (realItemId != COINS && !runData.itemPricesHA.containsKey(realItemId))
			{
				runData.itemPricesHA.put(realItemId, haPrice);
			}

			if (isNewRun)
			{
				if (runData.initialItemQtys.containsKey(realItemId))
				{
					runData.initialItemQtys.put(realItemId, runData.initialItemQtys.get(realItemId) + itemQty);
				}
				else
				{
					runData.initialItemQtys.put(realItemId, itemQty);
				}
			}

			if (runData.itemQtys.containsKey(realItemId))
			{
				runData.itemQtys.put(realItemId, runData.itemQtys.get(realItemId) + itemQty);
			}
			else
			{
				runData.itemQtys.put(realItemId, itemQty);
			}
		}

		long[] totals = new long[3];

		totals[TOTAL_GP_GE_INDEX] = totalGp;
		totals[TOTAL_GP_HA_INDEX] = totalGpHA;
		totals[TOTAL_QTY_INDEX] = totalQty;

		return totals;
	}

	long[] getEquipmentTotals(boolean isNewRun)
	{
		final ItemContainer itemContainer = client.getItemContainer(InventoryID.EQUIPMENT);

		if (itemContainer == null)
		{
			return new long [] {0, 0};
		}

		Item ammo = itemContainer.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());

		List<Integer> eIds = getEquipmentIds();

		long eTotal = 0;
		long eTotalHA = 0;
		for (long itemId: eIds)
		{
			long qty = 1;
			if (ammo != null && itemId == ammo.getId())
			{
				qty = ammo.getQuantity();
			}

			long gePrice;
			long haPrice;

			if (runData.itemPrices.containsKey(itemId))
			{
				gePrice = runData.itemPrices.get(itemId);
			}
			else
			{
				gePrice = itemManager.getItemPrice(Math.toIntExact(itemId));
			}

			if (runData.itemPricesHA.containsKey(itemId))
			{
				haPrice = runData.itemPricesHA.get(itemId);
			}
			else
			{
				ItemComposition itemComposition = itemManager.getItemComposition(Math.toIntExact(itemId));
				haPrice = itemComposition.getHaPrice();
			}

			long totalPrice = qty * gePrice;
			long totalPriceHA = qty * haPrice;

			eTotal += totalPrice;
			eTotalHA += totalPriceHA;

			if (!runData.itemPrices.containsKey(itemId))
			{
				runData.itemPrices.put(itemId, gePrice);
			}

			if (!runData.itemPricesHA.containsKey(itemId))
			{
				runData.itemPricesHA.put(itemId, haPrice);
			}

			if (isNewRun)
			{
				if (runData.initialItemQtys.containsKey(itemId))
				{
					runData.initialItemQtys.put(itemId, runData.initialItemQtys.get(itemId) + qty);
				}
				else
				{
					runData.initialItemQtys.put(itemId, qty);
				}
			}

			if (runData.itemQtys.containsKey(itemId))
			{
				runData.itemQtys.put(itemId, runData.itemQtys.get(itemId) + qty);
			}
			else
			{
				runData.itemQtys.put(itemId, qty);
			}
		}

		return new long [] {eTotal, eTotalHA};
	}

	private List<Integer> getEquipmentIds()
	{
		List<Item> equipment = getEquipment();

		return equipment
				.stream()
				.map(Item::getId)
				.collect(Collectors.toList());
	}

	private List<Item> getEquipment()
	{
		final ItemContainer equipment = client.getItemContainer(InventoryID.EQUIPMENT);

		if (equipment == null)
		{
			return new ArrayList<>();
		}

		Item head = equipment.getItem(EquipmentInventorySlot.HEAD.getSlotIdx());
		Item cape = equipment.getItem(EquipmentInventorySlot.CAPE.getSlotIdx());
		Item amulet = equipment.getItem(EquipmentInventorySlot.AMULET.getSlotIdx());
		Item ammo = equipment.getItem(EquipmentInventorySlot.AMMO.getSlotIdx());
		Item weapon = equipment.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		Item body = equipment.getItem(EquipmentInventorySlot.BODY.getSlotIdx());
		Item shield = equipment.getItem(EquipmentInventorySlot.SHIELD.getSlotIdx());
		Item legs = equipment.getItem(EquipmentInventorySlot.LEGS.getSlotIdx());
		Item gloves = equipment.getItem(EquipmentInventorySlot.GLOVES.getSlotIdx());
		Item boots = equipment.getItem(EquipmentInventorySlot.BOOTS.getSlotIdx());
		Item ring = equipment.getItem(EquipmentInventorySlot.RING.getSlotIdx());

		List<Item> items = new ArrayList<Item>();

		if (head != null)
		{
			items.add(head);
		}

		if (cape != null)
		{
			items.add(cape);
		}

		if (amulet != null)
		{
			items.add(amulet);
		}

		if (ammo != null)
		{
			items.add(ammo);
		}

		if (weapon != null)
		{
			items.add(weapon);
		}

		if (body != null)
		{
			items.add(body);
		}

		if (shield != null)
		{
			items.add(shield);
		}

		if (legs != null)
		{
			items.add(legs);
		}

		if (gloves != null)
		{
			items.add(gloves);
		}

		if (boots != null)
		{
			items.add(boots);
		}

		if (ring != null)
		{
			items.add(ring);
		}

		return items;
	}

	List<LiteUtilsTooltipItem> getInventoryLedger()
	{
		List<LiteUtilsTooltipItem> ledgerItems = new LinkedList<>();

		final ItemContainer itemContainer = overlay.getInventoryItemContainer();

		if (itemContainer == null)
		{
			return new LinkedList<>();
		}

		final Item[] items = itemContainer.getItems();

		final LinkedList<Item> allItems = new LinkedList<>(Arrays.asList(items));
		if (allItems.stream().anyMatch(s -> s.getId() == RUNEPOUCH_ITEM_ID || s.getId() == DIVINE_RUNEPOUCH_ITEM_ID))
		{
			allItems.addAll(getRunepouchContents());
		}

		Map<Long, Long> qtyMap = new HashMap<>();

		for (Item item: allItems)
		{
			long itemId = item.getId();

			final ItemComposition itemComposition = itemManager.getItemComposition(Math.toIntExact(itemId));

			String itemName = itemComposition.getName();
			final boolean ignore = runData.ignoredItems.stream().anyMatch(s -> {
				String lcItemName = itemName.toLowerCase();
				String lcS = s.toLowerCase();
				return lcItemName.contains(lcS);
			});
			if (ignore)
			{
				continue;
			}

			final boolean isNoted = itemComposition.getNote() != -1;
			final long realItemId = isNoted ? itemComposition.getLinkedNoteId() : itemId;

			long itemQty = item.getQuantity();

			if (qtyMap.containsKey(realItemId))
			{
				qtyMap.put(realItemId, qtyMap.get(realItemId) + itemQty);
			}
			else
			{
				qtyMap.put(realItemId, itemQty);
			}
		}

		for (Long itemId: qtyMap.keySet())
		{
			final ItemComposition itemComposition = itemManager.getItemComposition(Math.toIntExact(itemId));

			String itemName = itemComposition.getName();

			Long qty = qtyMap.get(itemId);

			Long total;
			if (config.priceType() == LiteUtilsPriceTypes.GRAND_EXCHANGE)
			{
				total = runData.itemPrices.get(itemId);
			}
			else
			{
				total = runData.itemPricesHA.get(itemId);
			}

			if (itemId == COINS || total == null)
			{
				total = 1L;
			}

			ledgerItems.add(new LiteUtilsTooltipItem(itemName, qty, total, itemId));
		}

		return ledgerItems;
	}

	List<LiteUtilsTooltipItem> getProfitLossLedger()
	{
		Map<Long, Long> prices;
		if (config.priceType() == LiteUtilsPriceTypes.GRAND_EXCHANGE)
		{
			prices = runData.itemPrices;
		}
		else
		{
			prices = runData.itemPricesHA;
		}

		Map<Long, Long> initialQtys = runData.initialItemQtys;
		Map<Long, Long> qtys = runData.itemQtys;

		Map<Long, Long> qtyDifferences = new HashMap<>();

		HashSet <Long> combinedQtyKeys = new HashSet<>();
		combinedQtyKeys.addAll(qtys.keySet());
		combinedQtyKeys.addAll(initialQtys.keySet());

		for (Long itemId: combinedQtyKeys)
		{
			Long initialQty = initialQtys.get(itemId);
			Long qty = qtys.get(itemId);

			if (initialQty == null)
			{
				initialQty = 0L;
			}

			if (qty == null)
			{
				qty = 0L;
			}

			qtyDifferences.put(itemId, qty - initialQty);
		}

		List<LiteUtilsTooltipItem> ledgerItems = new LinkedList<>();

		for (Long itemId: qtyDifferences.keySet())
		{
			final ItemComposition itemComposition = itemManager.getItemComposition(Math.toIntExact(itemId));
			Long price = prices.get(itemId);

			if (price == null)
			{
				price = 1L;
			}

			Long qtyDifference = qtyDifferences.get(itemId);

			List<LiteUtilsTooltipItem> filteredList = ledgerItems.stream().filter(
					item -> item.getDescription().equals(itemComposition.getName())).collect(Collectors.toList()
			);

			if (!filteredList.isEmpty())
			{
				filteredList.get(0).addQuantityDifference(qtyDifference);
			}
			else
			{
				if (price > 0)
				{
					ledgerItems.add(new LiteUtilsTooltipItem(itemComposition.getName(), qtyDifference, price, itemId));
				}
			}
		}

		return ledgerItems;
	}

	private List<Item> getRunepouchContents()
	{
		EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);
		List<Item> items = new ArrayList<>(RUNEPOUCH_AMOUNT_VARBITS.length);
		for (int i = 0; i < RUNEPOUCH_AMOUNT_VARBITS.length; i++)
		{
			long amount = client.getVarbitValue(Math.toIntExact(RUNEPOUCH_AMOUNT_VARBITS[i]));
			if (amount <= 0)
			{
				continue;
			}

			long runeId = client.getVarbitValue(Math.toIntExact(RUNEPOUCH_RUNE_VARBITS[i]));
			if (runeId == 0)
			{
				continue;
			}

			final long itemId = runepouchEnum.getIntValue(Math.toIntExact(runeId));
			Item item = new Item(Math.toIntExact(itemId), Math.toIntExact(amount));
			items.add(item);
		}
		return items;
	}

	void writeSavedData()
	{
		if (state == LiteUtilsState.BANK || Instant.now().toEpochMilli() - lastWriteSaveTime < 600)
		{
			return;
		}

		String profile = configManager.getRSProfileKey();

		String json = gson.toJson(runData);
		configManager.setConfiguration(LiteUtilsConfig.GROUP, profile, "inventory_total_data", json);

		lastWriteSaveTime = Instant.now().toEpochMilli();
	}

	private LiteUtilsTimerData getSavedData()
	{
		String profile = configManager.getRSProfileKey();
		String json = configManager.getConfiguration(LiteUtilsConfig.GROUP, profile, "inventory_total_data");

		LiteUtilsTimerData savedData = gson.fromJson(json, LiteUtilsTimerData.class);

		if (savedData == null)
		{
			return new LiteUtilsTimerData();
		}
		return savedData;
	}

	private LinkedList<String> getIgnoredItems()
	{
		return new LinkedList<>(
			Arrays.asList(
				config.ignoredItems().split("\\s*,\\s*")
			)
		);
	}


	void setMode(LiteUtilsModes mode)

	{
		this.mode = mode;
	}

	void setState(LiteUtilsState state)
	{
		this.prevState = this.state;
		this.state = state;
	}

	public LiteUtilsState getPreviousState()
	{
		return prevState;
	}

	public long getProfitGp()
	{
		if (mode == LiteUtilsModes.TOTAL)
		{
			return totalGp;
		}
		else if (config.priceType() == LiteUtilsPriceTypes.GRAND_EXCHANGE)
		{
			return totalGp - runData.profitLossInitialGp;
		}
		else
		{
			return totalGp - runData.profitLossInitialGpHA;
		}
	}

	void setTotalGp(long totalGp)
	{
		this.totalGp = totalGp;
	}

	void setTotalQty(long totalQty)
	{
		this.totalQty = totalQty;
	}

	public LiteUtilsModes getPLToggleOverride()
	{
		return plToggleOverride;
	}

	public boolean isManualNewRun()
	{
		if (manualNewRun)
		{
			manualNewRun = false;
			return true;
		}
		return false;
	}
}
