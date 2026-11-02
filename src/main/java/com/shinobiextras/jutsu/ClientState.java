package com.shinobiextras.jutsu;

/** Copia, no cliente, do que o servidor mandou (sem usar nenhuma classe exclusiva do cliente). */
public final class ClientState {
    private ClientState() {}

    private static volatile int mask;
    private static volatile int selected;

    public static void set(int m, int s) {
        mask = m;
        selected = s;
    }

    public static boolean isLearned(ExtraJutsu j) {
        return (mask & j.bit()) != 0;
    }

    public static int selected() {
        return selected;
    }
}
