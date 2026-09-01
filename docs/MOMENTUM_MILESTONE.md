# Basic Momentum milestone

Momentum is the first exercise-earned resource that can be deliberately spent on an RPG field action rather than only unlocking travel or level progression.

Implemented:

- Eligible walking steps grant Momentum through the existing monotonic fitness reward ledger.
- Prototype earning rate: 1 Momentum per 500 eligible steps.
- Total Momentum granted and total Momentum spent persist separately; available Momentum is `granted - spent` and never goes below zero.
- Health-provider corrections and repeated observations cannot remint already-accounted Momentum.
- Home shows the current Momentum balance and the prototype earning rate alongside the existing walking rewards.
- Adventure exposes a dedicated Momentum card.
- Prototype Rally costs 10 Momentum.
- Rally restores 25% of max HP and 25% of max MP to every conscious active party member, clamped to each maximum.
- Rally does not revive KO'd party members.
- Rally is disabled when the conscious party cannot benefit or the player cannot afford the cost.
- Rally plans recovery before requesting the ledger spend; an invalid/no-op recovery therefore does not spend Momentum.
- Momentum earning, spending, insufficient-balance handling, Rally recovery, KO behavior, and provider-correction behavior have pure regression tests.

Migration behavior:

- Existing characters do not receive a retroactive Momentum windfall for 500-step thresholds that were already processed before this feature existed.
- After upgrading, Momentum begins accruing when the character crosses new 500-step reward thresholds. This preserves the existing reward ledger's one-way accounting rather than replaying old exercise as a new currency.

Prototype tuning:

- `FitnessRewardEngine.PROTOTYPE_STEPS_PER_MOMENTUM = 500`
- `MomentumRules.RALLY_COST = 10`
- `MomentumRules.RALLY_RECOVERY_PERCENT = 25`
- Momentum currently has no storage cap or daily earning cap. Those are balance decisions for a later tuning pass.

Current limitation:

- Momentum lives in the fitness reward ledger while party vitals live in the RPG vitals store. Rally validates first and spends only on a valid plan, but the spend and vitals write are not yet one atomic database transaction. The later unified save/database layer should make cross-system spends atomic.
- This milestone intentionally adds one field use for Momentum. Battle-specific Momentum techniques, temporary buffs, combo systems, and other spend choices remain future work.
