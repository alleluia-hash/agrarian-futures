# Agrarian Futures (Fabric 1.20.1)

A fluctuating farm-goods market. Craft a **Ticker Board** and sell your crops, leather and iron to it for emeralds.

Recipe: iron ingots on top, redstone / emerald / redstone in the middle, planks on the bottom.

## How it works
- **Right-click** the board with an empty hand to see today's prices.
- **Right-click holding goods** to sell that stack.
- **Sneak with empty hands** to sell everything sellable in chests and barrels touching the board.
- Prices re-roll every **3 in-game days**, between 40% and 200% of the base price. They are the same for every player in the world.
- Every item you sell pushes that price down a little (up to -50%).
- Flood one good (**1728 items**, 27 stacks, in one price period) and its market **crashes**: it pays **nothing for 7 days**, for everyone. The board announces who did it. Goods that aren't bought stay in your chest.
- Sold: wheat, carrots, potatoes, beetroot, pumpkins, melon slices, sugar cane, leather, feathers, eggs, iron ingots.
- Renamed or enchanted items are never bought.
- Payment is in emeralds. Fractions are carried over per player, so small sales still add up.

Tuning knobs (base prices, swing, glut limit, crash length) are constants in `Commodity.java` and `Market.java`.

## Build
- PC: `gradle wrapper --gradle-version 8.6` once, then `./gradlew build`. Jar: `build/libs/agrarianfutures-1.0.0.jar`.
- No PC: push to GitHub. The workflow builds it and the jar is under the run's Artifacts.

Install the jar with Fabric API 0.92.x for 1.20.1 in `mods`.
