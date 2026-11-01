package com.agrarianfutures;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** The market rules: price rolls, selling pressure and crashes. */
public final class Market {
    public static final long DAY = 24000L;
    /** Prices re-roll every 3 in-game days. */
    public static final long PERIOD = 3 * DAY;
    /** A crashed commodity pays nothing for 7 in-game days. */
    public static final long CRASH_LENGTH = 7 * DAY;
    /** Items of one kind that can be sold in a single period before its price collapses. */
    public static final int GLUT = 1728;
    /** Prices swing between these multiples of the base price. */
    public static final double MIN_MULT = 0.4;
    public static final double MAX_MULT = 2.0;

    private Market() {
    }

    public static final class Result {
        public final int sold;
        public final double value;
        public final boolean crashed;

        Result(int sold, double value, boolean crashed) {
            this.sold = sold;
            this.value = value;
            this.crashed = crashed;
        }
    }

    public static ServerWorld overworld(ServerWorld w) {
        return w.getServer().getOverworld();
    }

    public static long epoch(long time) {
        return Math.floorDiv(time, PERIOD);
    }

    public static MarketState state(ServerWorld w) {
        ServerWorld ow = overworld(w);
        MarketState st = ow.getPersistentStateManager()
                .getOrCreate(MarketState::fromNbt, MarketState::new, "agrarianfutures_market");
        long t = ow.getTimeOfDay();
        st.refresh(t, epoch(t));
        return st;
    }

    /** This period's price multiplier for a commodity, the same for everyone in the world. */
    public static double mult(ServerWorld w, long epoch, int idx) {
        long h = overworld(w).getSeed() ^ (epoch * 0x9E3779B97F4A7C15L) ^ ((idx + 1) * 0xC2B2AE3D27D4EB4FL);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        double u = (h >>> 11) * (1.0 / (1L << 53));
        return MIN_MULT + (MAX_MULT - MIN_MULT) * u;
    }

    public static boolean isCrashed(ServerWorld w, MarketState st, Commodity c) {
        return overworld(w).getTimeOfDay() < st.crashUntil[c.ordinal()];
    }

    /** Price for one item right now, including the drop caused by what has already been sold. */
    public static double unitPrice(ServerWorld w, MarketState st, Commodity c) {
        if (isCrashed(w, st, c)) {
            return 0.0;
        }
        int i = c.ordinal();
        long t = overworld(w).getTimeOfDay();
        return c.base * mult(w, epoch(t), i) * (1.0 - 0.5 * st.sold[i] / GLUT);
    }

    /**
     * Sells up to count items. Each item sold lowers the price a little, and once the glut limit is
     * reached the market crashes and refuses the rest. Returns how many were taken and what they paid.
     */
    public static Result sell(ServerWorld w, MarketState st, Commodity c, int count) {
        ServerWorld ow = overworld(w);
        int i = c.ordinal();
        long t = ow.getTimeOfDay();
        if (t < st.crashUntil[i]) {
            return new Result(0, 0.0, false);
        }
        int room = GLUT - st.sold[i];
        int k = Math.min(count, room);
        if (k <= 0) {
            return new Result(0, 0.0, false);
        }
        double p = c.base * mult(w, epoch(t), i);
        double s = st.sold[i];
        double value = p * (k - 0.5 * (k * s + k * (k - 1) / 2.0) / GLUT);
        st.sold[i] += k;
        boolean crashed = false;
        if (st.sold[i] >= GLUT) {
            st.crashUntil[i] = t + CRASH_LENGTH;
            crashed = true;
        }
        st.markDirty();
        return new Result(k, value, crashed);
    }

    public static String fmt(double v) {
        return String.format(Locale.ROOT, "%.2f", v);
    }

    /** The lines shown when a player checks the board. */
    public static List<Text> board(ServerWorld w) {
        ServerWorld ow = overworld(w);
        MarketState st = state(w);
        long t = ow.getTimeOfDay();
        long ep = epoch(t);
        List<Text> out = new ArrayList<>();
        double daysLeft = ((ep + 1) * PERIOD - t) / (double) DAY;
        out.add(Text.literal("== Ticker Board ==").formatted(Formatting.GOLD, Formatting.BOLD));
        out.add(Text.literal(String.format(Locale.ROOT, "Day %d. New prices in %.1f days. Prices in emeralds.",
                t / DAY, daysLeft)).formatted(Formatting.GRAY));
        for (Commodity c : Commodity.values()) {
            int i = c.ordinal();
            MutableText line = c.item.getName().copy().formatted(Formatting.WHITE);
            if (t < st.crashUntil[i]) {
                double d = (st.crashUntil[i] - t) / (double) DAY;
                line.append(Text.literal(String.format(Locale.ROOT, "  CRASHED, no buyers for %.1f more days", d))
                        .formatted(Formatting.RED));
            } else {
                double m = mult(w, ep, i);
                long pct = Math.round((m - 1.0) * 100.0);
                line.append(Text.literal("  " + fmt(unitPrice(w, st, c)) + " ").formatted(Formatting.YELLOW));
                if (m >= 1.05) {
                    line.append(Text.literal("▲ +" + pct + "%").formatted(Formatting.GREEN));
                } else if (m <= 0.95) {
                    line.append(Text.literal("▼ " + pct + "%").formatted(Formatting.RED));
                } else {
                    line.append(Text.literal("▬ " + pct + "%").formatted(Formatting.GRAY));
                }
                if (st.sold[i] > 0) {
                    line.append(Text.literal("  glut " + (100 * st.sold[i] / GLUT) + "%")
                            .formatted(Formatting.DARK_GRAY));
                }
            }
            out.add(line);
        }
        out.add(Text.literal("Hold goods and right-click to sell them. Sneak with empty hands to sell the contents of "
                + "chests and barrels next to the board.").formatted(Formatting.DARK_GRAY));
        return out;
    }
}
