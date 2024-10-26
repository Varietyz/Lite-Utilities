package com.liteutilities;

import lombok.Getter;

@Getter
public class LiteUtilsTooltipItem
{
    private final String description;
    private long qty;
    private final long amount;
	@Getter
	private long itemId;

    public LiteUtilsTooltipItem(String description, long qty, long amount, long itemId)
    {
        this.description = description;
        this.qty = qty;
        this.amount = amount;
		this.itemId = itemId;
    }

	public void addQuantityDifference(long qtyDifference)
    {
        qty += qtyDifference;
    }
}
