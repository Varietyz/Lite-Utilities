package com.liteutilities;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LiteUtilitiesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(LiteUtilitiesPlugin.class);
		RuneLite.main(args);
	}
}