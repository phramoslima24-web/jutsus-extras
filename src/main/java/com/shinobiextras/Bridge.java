package com.shinobiextras;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Ponte por reflexao para as variaveis do jogador do Narutoto (chakra etc.).
 * Se algo nao puder ser lido, o addon simplesmente nao cobra chakra.
 */
public final class Bridge {
    private Bridge() {}

    private static Capability<?> cap;
    private static boolean capTried;
    private static final Map<String, Field> FIELDS = new HashMap<>();

    private static Capability<?> cap() {
        if (!capTried) {
            capTried = true;
            try {
                Class<?> c = Class.forName("net.mcreator.narutoto.network.NarutotoModVariables");
                cap = (Capability<?>) c.getField("PLAYER_VARIABLES_CAPABILITY").get(null);
            } catch (Throwable t) {
                System.err.println("[shinobiextras] Nao achei PLAYER_VARIABLES_CAPABILITY: " + t);
            }
        }
        return cap;
    }

    private static Object vars(Entity e) {
        Capability<?> c = cap();
        if (c == null || e == null) return null;
        LazyOptional<?> lo = e.getCapability(c);
        return lo.resolve().orElse(null);
    }

    private static Field field(Object vars, String name) throws NoSuchFieldException {
        Field f = FIELDS.get(name);
        if (f == null) {
            f = vars.getClass().getField(name);
            FIELDS.put(name, f);
        }
        return f;
    }

    public static double getDouble(Entity e, String name, double def) {
        try {
            Object v = vars(e);
            return v == null ? def : field(v, name).getDouble(v);
        } catch (Throwable t) {
            return def;
        }
    }

    public static boolean setDouble(Entity e, String name, double value) {
        try {
            Object v = vars(e);
            if (v == null) return false;
            field(v, name).setDouble(v, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void sync(Entity e) {
        try {
            Object v = vars(e);
            if (v != null) v.getClass().getMethod("syncPlayerVariables", Entity.class).invoke(v, e);
        } catch (Throwable t) {
            System.err.println("[shinobiextras] sync falhou: " + t);
        }
    }

    /** Soma (ou subtrai, se negativo) chakra do jogador. Devolve false se nao deu para ler/escrever. */
    public static boolean addChakra(Entity e, double delta) {
        double cur = getDouble(e, "chakra", -1);
        if (cur < 0) return false;
        double max = getDouble(e, "chakra_maximum", -1);
        double next = Math.max(0, cur + delta);
        if (delta > 0 && max > 0) next = Math.min(next, Math.max(max, cur));
        if (!setDouble(e, "chakra", next)) return false;
        sync(e);
        return true;
    }
}
