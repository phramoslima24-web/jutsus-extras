package com.shinobiextras.jutsu;

import javax.annotation.Nullable;

/** Jutsus extras que o Narutoto 0.4.1 nao tem. */
public enum ExtraJutsu {
    //                id                  nivel XP  recarga(ticks)  chakra
    KIRIGAKURE("kirigakure",               15,      600,            300),
    NIKUDAN_SENSHA("nikudan_sensha",       12,      300,            200),
    GATSUUGA("gatsuuga",                   14,      240,            250),
    SHIKOTSUMYAKU("shikotsumyaku",         20,      400,            400),
    KIKAICHU("kikaichu",                   10,      300,            150),
    BAKUHA("bakuha",                       25,      500,            500),
    SAMEHADA("samehada",                   18,      360,            250),
    CHIDORI_NAGASHI("chidori_nagashi",     22,      600,            400),
    SHUNSHIN("shunshin",                    8,       80,            100);

    public final String id;
    public final int xpCost;
    public final int cooldownTicks;
    public final int chakraCost;

    ExtraJutsu(String id, int xpCost, int cooldownTicks, int chakraCost) {
        this.id = id;
        this.xpCost = xpCost;
        this.cooldownTicks = cooldownTicks;
        this.chakraCost = chakraCost;
    }

    public int bit() {
        return 1 << ordinal();
    }

    public String langKey() {
        return "jutsu.shinobiextras." + id;
    }

    @Nullable
    public static ExtraJutsu byIndex(int i) {
        ExtraJutsu[] v = values();
        return i >= 0 && i < v.length ? v[i] : null;
    }

    @Nullable
    public static ExtraJutsu byId(String id) {
        for (ExtraJutsu j : values()) if (j.id.equals(id)) return j;
        return null;
    }
}
