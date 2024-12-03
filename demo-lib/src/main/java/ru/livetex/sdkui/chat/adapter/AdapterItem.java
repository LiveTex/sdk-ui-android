package ru.livetex.sdkui.chat.adapter;

public interface AdapterItem extends Comparable<AdapterItem> {
	ItemType getAdapterItemType();
}