package com.eternalcode.parcellockers.shared;

import com.eternalcode.commons.scheduler.Scheduler;
import de.rapha149.signgui.SignEditor;
import de.rapha149.signgui.SignGUI;
import de.rapha149.signgui.SignGUIAction;
import org.bukkit.entity.Player;

public class ScheduledGuiAction implements SignGUIAction {

    private final Scheduler scheduler;
    private final Runnable runnable;

    public ScheduledGuiAction(Scheduler scheduler, Runnable runnable) {
        this.scheduler = scheduler;
        this.runnable = runnable;
    }

    @Override
    public SignGUIActionInfo getInfo() {
        return new SignGUIActionInfo("scheduledGuiAction", false, 0);
    }

    @Override
    public void execute(SignGUI gui, SignEditor signEditor, Player player) {
        this.scheduler.run(this.runnable);
    }
}
