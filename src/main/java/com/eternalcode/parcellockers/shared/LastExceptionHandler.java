package com.eternalcode.parcellockers.shared;

import com.eternalcode.parcellockers.parcel.Parcel;

import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class LastExceptionHandler implements BiConsumer<Optional<List<Parcel>>, Throwable> {

    @Override
    public void accept(Optional<List<Parcel>> result, Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
