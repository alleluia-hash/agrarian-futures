package com.agrarianfutures;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;

/**
 * World-wide market data, saved with the world.
 * Prices themselves are not stored: they are derived from the world seed and the current
 * 3-day period, so only sales and crashes need saving.
 */
public class MarketState extends PersistentState {
    private static final int N = Commodity.values().length;

    public long lastEpoch = Long.MIN_VALUE;
    public final int[] sold = new int[N];
    public final long[] crashUntil = new long[N];
    public final Map<UUID, Double> credit = new HashMap<>();

    public static MarketState fromNbt(NbtCompound nbt) {
        MarketState s = new MarketState();
        if (nbt.contains("lastEpoch")) {
            s.lastEpoch = nbt.getLong("lastEpoch");
        }
        int[] soldIn = nbt.getIntArray("sold");
        long[] crashIn = nbt.getLongArray("crashUntil");
        for (int i = 0; i < N; i++) {
            if (i < soldIn.length) {
                s.sold[i] = soldIn[i];
            }
            if (i < crashIn.length) {
                s.crashUntil[i] = crashIn[i];
            }
        }
        NbtCompound c = nbt.getCompound("credit");
        for (String key : c.getKeys()) {
            try {
                s.credit.put(UUID.fromString(key), c.getDouble(key));
            } catch (IllegalArgumentException ignored) {
                // skip a corrupt key
            }
        }
        return s;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putLong("lastEpoch", lastEpoch);
        nbt.putIntArray("sold", sold);
        nbt.putLongArray("crashUntil", crashUntil);
        NbtCompound c = new NbtCompound();
        for (Map.Entry<UUID, Double> e : credit.entrySet()) {
            c.putDouble(e.getKey().toString(), e.getValue());
        }
        nbt.put("credit", c);
        return nbt;
    }

    /** Ends finished crashes and starts a fresh set of sales counters when a new price period begins. */
    public void refresh(long time, long epoch) {
        boolean dirty = false;
        for (int i = 0; i < N; i++) {
            if (crashUntil[i] > 0 && time >= crashUntil[i]) {
                crashUntil[i] = 0;
                sold[i] = 0;
                dirty = true;
            }
        }
        if (epoch != lastEpoch) {
            for (int i = 0; i < N; i++) {
                if (crashUntil[i] == 0) {
                    sold[i] = 0;
                }
            }
            lastEpoch = epoch;
            dirty = true;
        }
        if (dirty) {
            markDirty();
        }
    }

    /** Adds earnings (in emeralds, fractional) and returns the whole emeralds to pay out now. */
    public int addCredit(UUID player, double amount) {
        double total = credit.getOrDefault(player, 0.0) + amount;
        int whole = (int) Math.floor(total);
        credit.put(player, total - whole);
        markDirty();
        return whole;
    }
}
