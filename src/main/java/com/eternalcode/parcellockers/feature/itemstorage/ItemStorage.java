package com.eternalcode.parcellockers.feature.itemstorage;

import java.util.List;
import java.util.UUID;

public record ItemStorage(UUID owner, List<String> serializedItemStacks) {

    /*public void add(ItemStack itemStack) {
        this.serializedItemStacks.add(ItemUtil.itemStackToString(itemStack));
    }

    public void add(String string) {
        this.serializedItemStacks.add(string);
    }

    public void remove(ItemStack itemStack) {
        this.serializedItemStacks.remove(ItemUtil.itemStackToString(itemStack));
    }

    public void remove(String string) {
        this.serializedItemStacks.remove(string);
    }

    public boolean contains(ItemStack itemStack) {
        return this.serializedItemStacks.contains(ItemUtil.itemStackToString(itemStack));
    }

    public boolean contains(String string) {
        return this.serializedItemStacks.contains(string);
    }

    public boolean belongsTo(UUID uuid) {
        return this.owner.equals(uuid);
    }*/

}
