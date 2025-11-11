package com.goldwithdrawamount;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class GoldWithdrawAmountPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GoldWithdrawAmountPlugin.class);
		RuneLite.main(args);
	}
}