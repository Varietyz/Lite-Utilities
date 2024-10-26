package com.liteutilities;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class LiteUtilsTimerData
{
    long profitLossInitialGp = 0;
    long profitLossInitialGpHA = 0;

    Map<Long, Long> itemPrices = new HashMap<>();
    Map<Long, Long> itemPricesHA = new HashMap<>();
    Map<Long, Long> initialItemQtys = new HashMap<>();
    Map<Long, Long> itemQtys = new HashMap<>();

    LinkedList<String> ignoredItems = new LinkedList<>();
}
