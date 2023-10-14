package com.eternalcode.parcellockers.locker.repository;

import com.eternalcode.parcellockers.locker.Locker;

import java.util.List;

public record LockerPageResult(List<Locker> lockers, boolean hasNextPage) {

}
