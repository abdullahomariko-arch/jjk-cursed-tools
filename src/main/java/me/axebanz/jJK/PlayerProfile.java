package me.axebanz.jJK;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerProfile {
    public final UUID uuid;

    public int ce = 10;

    public String techniqueId = null;
    public boolean techniqueEnabled = false;

    public long nullifiedUntilMs = 0L;

    public final Map<String, Long> cooldowns = new HashMap<>();

    public long regenLockedUntilMs = 0L;

    public long isohReapplyUntilMs = 0L;
    public String isohReapplyTargetUuid = null;

    // ===== Divine Wheel =====
    public String wheelMode = "MELEE";
    public final Map<String, Integer> wheelStacks = new HashMap<>();

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }
}
