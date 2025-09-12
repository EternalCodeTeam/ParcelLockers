package com.eternalcode.parcellockers.gui.implementation.locker;

import com.eternalcode.parcellockers.parcel.ParcelSize;
import com.eternalcode.parcellockers.parcel.ParcelStatus;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(fluent = true)
public class SendingGuiState {

    private String parcelName;
    private String parcelDescription;
    private ParcelSize size;
    private UUID receiver;
    private boolean priority;
    private UUID entryLocker;
    private UUID destinationLocker;
    private ParcelStatus status;

    public SendingGuiState() {
        this.parcelName = null;
        this.parcelDescription = "";
        this.size = ParcelSize.SMALL;
        this.receiver = null;
        this.priority = false;
        this.entryLocker = UUID.randomUUID();
        this.destinationLocker = null;
        this.status = ParcelStatus.IN_PROGRESS;
    }

}
