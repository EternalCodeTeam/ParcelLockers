package com.eternalcode.parcellockers.parcellocker.repository;

import com.eternalcode.parcellockers.parcellocker.ParcelLocker;

import java.util.Set;

public record ParcelLockerPageResult(Set<ParcelLocker> parcelLockers, boolean hasNextPage) {

}
