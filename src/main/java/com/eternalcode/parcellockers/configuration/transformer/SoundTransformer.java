package com.eternalcode.parcellockers.configuration.transformer;

import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import lombok.NonNull;
import org.bukkit.Sound;
import org.jetbrains.annotations.NotNull;

public class SoundTransformer extends BidirectionalTransformer<Sound, String> {

    @Override
    public GenericsPair<Sound, String> getPair() {
        return this.genericsPair(Sound.class, String.class);
    }

    @Override
    public String leftToRight(@NotNull Sound data, @NonNull SerdesContext serdesContext) {
        return data.name();
    }

    @Override
    public Sound rightToLeft(@NonNull String data, @NonNull SerdesContext serdesContext) {
        return Sound.valueOf(data);
    }
}
