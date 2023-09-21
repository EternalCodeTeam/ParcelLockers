package com.eternalcode.parcellockers.parcellocker.repository;

import com.eternalcode.parcellockers.parcellocker.ParcelLocker;

import java.util.List;

public record ParcelLockerPageResult(List<ParcelLocker> parcelLockers, boolean hasNextPage) {

}
