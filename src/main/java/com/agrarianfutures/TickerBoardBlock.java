package com.agrarianfutures;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

/**
 * The Ticker Board.
 * Right-click with nothing sellable: show prices.
 * Right-click holding goods: sell that stack.
 * Sneak with empty hands: sell everything sellable in chests and barrels touching the board.
 */
public class TickerBoardBlock extends Block {
    public TickerBoardBlock(Settings settings) {
        super(settings);
    }

    /** Totals for one sale, so the player gets one summary message. */
    private static final class Sale {
        final int[] sold = new int[Commodity.values().length];
        final List<Commodity> crashed = new ArrayList<>();
        double value;
        int total;

        void add(Commodity c, Market.Result r) {
            sold[c.ordinal()] += r.sold;
            total += r.sold;
            value += r.value;
            if (r.crashed && !crashed.contains(c)) {
                crashed.add(c);
            }
        }
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand,
                              BlockHitResult hit) {
        if (hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        ServerWorld sw = (ServerWorld) world;
        ServerPlayerEntity sp = (ServerPlayerEntity) player;
        MarketState st = Market.state(sw);
        ItemStack held = player.getMainHandStack();

        if (player.isSneaking()) {
            Sale sale = new Sale();
            for (Direction d : Direction.values()) {
                BlockPos np = pos.offset(d);
                BlockState ns = world.getBlockState(np);
                Inventory inv = null;
                if (ns.getBlock() instanceof ChestBlock chest) {
                    inv = ChestBlock.getInventory(chest, ns, world, np, true);
                } else if (ns.getBlock() instanceof BarrelBlock && world.getBlockEntity(np) instanceof Inventory b) {
                    inv = b;
                }
                if (inv != null) {
                    sellFromInventory(sw, st, inv, sale);
                }
            }
            finish(sw, sp, st, sale, pos, "Put chests or barrels next to the board, then sneak with empty hands.");
        } else if (Commodity.of(held) != null) {
            Sale sale = new Sale();
            Commodity c = Commodity.of(held);
            Market.Result r = Market.sell(sw, st, c, held.getCount());
            if (r.sold > 0) {
                held.decrement(r.sold);
                sale.add(c, r);
            }
            finish(sw, sp, st, sale, pos, "No buyers for that right now.");
        } else {
            for (Text line : Market.board(sw)) {
                sp.sendMessage(line, false);
            }
        }
        return ActionResult.SUCCESS;
    }

    private static void sellFromInventory(ServerWorld sw, MarketState st, Inventory inv, Sale sale) {
        for (int slot = 0; slot < inv.size(); slot++) {
            ItemStack stack = inv.getStack(slot);
            Commodity c = Commodity.of(stack);
            if (c == null) {
                continue;
            }
            Market.Result r = Market.sell(sw, st, c, stack.getCount());
            if (r.sold <= 0) {
                continue;
            }
            stack.decrement(r.sold);
            if (stack.isEmpty()) {
                inv.setStack(slot, ItemStack.EMPTY);
            }
            sale.add(c, r);
        }
        inv.markDirty();
    }

    private static void finish(ServerWorld sw, ServerPlayerEntity sp, MarketState st, Sale sale, BlockPos pos,
                               String nothingMessage) {
        if (sale.total == 0) {
            sp.sendMessage(Text.literal(nothingMessage).formatted(Formatting.GRAY), false);
            return;
        }
        int emeralds = st.addCredit(sp.getUuid(), sale.value);
        int left = emeralds;
        while (left > 0) {
            int n = Math.min(left, 64);
            ItemStack pay = new ItemStack(Items.EMERALD, n);
            if (!sp.getInventory().insertStack(pay)) {
                sp.dropItem(pay, false);
            }
            left -= n;
        }
        for (Commodity c : Commodity.values()) {
            int n = sale.sold[c.ordinal()];
            if (n > 0) {
                sp.sendMessage(Text.literal("Sold ").formatted(Formatting.GRAY)
                        .append(Text.literal(n + "x ").formatted(Formatting.WHITE))
                        .append(c.item.getName().copy().formatted(Formatting.WHITE)), false);
            }
        }
        sp.sendMessage(Text.literal("Paid " + emeralds + " emeralds (earned " + Market.fmt(sale.value)
                + ", the fraction is carried over).").formatted(Formatting.GREEN), false);
        sw.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.BLOCKS, 1.0f, 1.0f);

        for (Commodity c : sale.crashed) {
            sw.getServer().getPlayerManager().broadcast(
                    Text.literal("MARKET CRASH! ").formatted(Formatting.RED, Formatting.BOLD)
                            .append(sp.getName().copy().formatted(Formatting.WHITE))
                            .append(Text.literal(" flooded the market. ").formatted(Formatting.RED))
                            .append(c.item.getName().copy().formatted(Formatting.WHITE))
                            .append(Text.literal(" is worth nothing for 7 days.").formatted(Formatting.RED)),
                    false);
        }
    }
}
