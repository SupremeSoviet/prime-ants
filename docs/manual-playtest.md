# Manual Playtest Checklist

1. Start `./gradlew runClient`.
2. Create a creative world with cheats enabled.
3. Run `/give @p formic_frontier:queen_egg` and `/give @p formic_frontier:colony_tablet`.
4. Use the Queen Egg on grass or stone and confirm a central mound, four chamber buildings, paths, resource clusters, queen, and castes appear.
5. Right-click the mound, chamber buildings, and resource clusters; each should print colony status in chat.
6. Right-click an ant and confirm it prints caste, colony id, health, and upkeep.
7. Watch workers/miners walk to food/chitin/ore nodes, pause, return to chambers, and update the current task/resources.
8. Run `/formic colony dump` and confirm food, ore, chitin, resin, fungus, venom, knowledge, caste counts, chambers, queen health, culture, and instinct are printed.
9. Use the Colony Tablet and confirm the tabbed status screen opens with Overview, Instinct, Buildings, Requests, Research, Trade, Diplomacy, Relations, and Events.
10. Run `/formic colony tick 200` and confirm resources and caste counts change.
11. Run `/formic colony resource set food 0`, then `/formic colony tick 100`, and confirm growth slows or stops.
12. Run `/formic ant spawn giant` and confirm a large expensive caste spawns.
13. Press instinct buttons in the Instinct tab or run `/formic colony instinct defense`; confirm the autonomous instinct and current task update.
14. Use `formic_frontier:pheromone_dust` on a miner/soldier/worker and confirm the colony instinct changes to that caste's focus.
15. Hand-feed wheat/raw biomass/chitin shards to ants and royal jelly to the queen; confirm resources/reputation/events update.
16. Wait or run `/formic colony tick 400` and confirm queued chitin farm/market/watch post construction progresses.
17. Right-click the market chamber or use the Colony Tablet; confirm the Trade tab has buttons for selling resources and buying spores/dust/resin/seals/banners/armor.
18. Craft `resin_glob`, `chitin_plate`, `colony_seal`, `war_banner`, `diplomacy_shrine`, `fungus_culture`, `venom_sac`, `mandible_plate`, and `royal_wax`; confirm recipes unlock and icons match the ant-colony texture style.
19. Put wheat/chitin/tokens in inventory and test `/formic trade sell_wheat`, `/formic trade sell_chitin`, and `/formic trade buy_chitin_spore`.
20. Use `/formic diplomacy envoy` near an allied colony after seeding rivals; confirm relation moves one step toward peace and tokens/dust are spent.
20. Use `/formic research resin_masonry`, then tick the colony and confirm the Research tab completes it and Requests opens any missing-resource bottlenecks.
20. Build or wait for `pheromone_archive`, `resin_depot`, `fungus_garden`, `armory`, and eventually `venom_press`; confirm new blocks render and colony resources change.
20. Buy or craft resin chitin armor, mandible saber, and venom spear after the relevant research unlocks.
21. Build reputation/rank, then test `/formic diplomacy tribute`, `/formic diplomacy truce`, `/formic diplomacy incite`, and eventually `/formic diplomacy war_pact`.
22. Confirm colony rank progresses from Outpost toward Burrow/Hive/Citadel as buildings, population, reputation, and allied relations grow.
23. Plant `formic_frontier:chitin_spore` on dirt/rooted dirt/mud near the colony and confirm the chitin bed ages and harvests into chitin shards.
24. Run `/formic colony seed-rivals 2 64`, then `/formic colony tick 2000`; confirm rival relations appear, raids log events, and watch posts reduce losses without breaking buildings.
25. Craft/equip chitin armor pieces and confirm they now require chitin plates rather than raw fiber.
26. Save, quit, reopen the world, and confirm `/formic colony dump` still shows colonies, ranks, buildings, relations, resources, event log, and queued work.
