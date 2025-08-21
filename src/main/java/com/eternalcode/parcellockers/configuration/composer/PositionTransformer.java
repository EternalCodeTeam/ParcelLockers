package com.eternalcode.parcellockers.configuration.composer;

import com.eternalcode.parcellockers.shared.Position;
import eu.okaeri.configs.schema.GenericsPair;
import eu.okaeri.configs.serdes.BidirectionalTransformer;
import eu.okaeri.configs.serdes.SerdesContext;
import org.jetbrains.annotations.NotNull;

public class PositionTransformer extends BidirectionalTransformer<Position, String> {

    @Override
    public GenericsPair<Position, String> getPair() {
        return this.genericsPair(Position.class, String.class);
    }

    @Override
    public String leftToRight(@NotNull Position position, @NotNull SerdesContext context) {
        return position.toString();
    }

    @Override
    public Position rightToLeft(@NotNull String data, @NotNull SerdesContext context) {
        return Position.parse(data);
    }
}
