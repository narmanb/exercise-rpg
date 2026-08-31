# Exercise RPG — Current Approved Roadmap

_Last updated: August 31, 2026_

## Core Concept

Exercise RPG is a native Android RPG where real-world activity feeds an actual game rather than forcing the player to exercise during combat.

Real-world activity provides resources, progression, and access to adventures. The player can then play the RPG normally whenever they want.

## Platform / Technical Direction

- Native Android implementation, likely Kotlin.
- Local persistent save/database is the authority for RPG state.
- Health Connect is an optional fitness-data bridge and historical activity source.
- Direct Android step tracking is used for live responsiveness and as a fallback.
- Activity Recognition is used as a supporting signal for walking/running/in-vehicle detection.
- The game must remain usable if Health Connect is unavailable or permission is denied.
- Detect device/Android capabilities automatically and choose the best available tracking system.

## Player-Visible Tracking Status

The player should be able to see, without unnecessary complexity:

- Health Connect: connected / disconnected / unsupported
- Device/local step tracking: active / unavailable
- Activity Recognition: active / unavailable where relevant
- Current tracking mode being used

This can live in a small Fitness Tracking / Data Sources status screen.

## Development / Debug Step Screen

During development and testing, expose multiple counters side-by-side so discrepancies can be diagnosed.

Recommended debug values:

- In-game displayed steps
- Eligible/rewarded steps
- Health Connect step total for the relevant period
- Direct/local sensor step delta
- Unsynced live steps
- Last Health Connect sync time
- Current / recent Activity Recognition state
- Current activity confidence when available
- Character fitness baseline timestamp
- Steps rejected or held because of a strong IN_VEHICLE signal
- Current Android/API capability mode

Once tracking is proven reliable, the normal player UI should primarily show the single in-game step total.

## Character-Creation Fitness Baseline

Real-world activity only becomes eligible for RPG rewards after the player creates a character.

At character creation, save:

- Exact character creation timestamp
- Current Health Connect step baseline for the relevant day/period
- Current direct step-counter baseline if available

Any Health Connect history before this point gives:

- 0 RPG XP
- 0 Adventure Points
- 0 other exercise-derived RPG rewards

Example:

Health Connect already contains 100,000,000 lifetime steps.

A new character is created.

Those 100,000,000 historical steps grant nothing.

Only activity occurring after the character's fitness epoch can generate rewards.

This also prevents reinstalling and creating a new character from granting old Health Connect history again.

## Step Tracking Architecture

### Health Connect

Use Health Connect as the durable/historical fitness record when supported and connected.

Potentially read:

- Steps
- Distance
- Exercise sessions
- Speed
- Cadence
- Calories burned
- Other approved fitness data later

Do not use Health Connect as the RPG save.

### Direct Android Step Tracking

Use the direct/local Android step system for:

- Immediate live step updates
- A fallback when Health Connect cannot provide native phone step collection
- Filling the responsiveness gap while Health Connect data is batched/delayed

The raw hardware step counter must never be treated as a permanent lifetime game total because its baseline may reset after a device reboot.

### Reconciliation

Do not add Health Connect steps and direct-sensor steps together blindly.

Conceptually track:

- Health Connect confirmed steps
- Live unsynced direct-sensor steps
- Eligible/rewarded steps
- Rejected/withheld steps where applicable

Example:

Health Connect confirmed: 5,000
Live new direct steps: +120
Displayed total: 5,120

When Health Connect later reaches 5,120, the live +120 becomes confirmed instead of being added a second time.

## Reboots / Cache

- Device reboot may reset the raw direct sensor baseline.
- This must not reset the in-game fitness total or rewards.
- Health Connect and the RPG synchronization ledger are used to reconcile after reboot.
- Clearing Android app cache must not delete RPG save data.
- Persistent game data must never live in cache storage.
- Clearing app storage/data or uninstalling can remove local RPG save data unless restored from backup.

## Save / Backup Direction

The local RPG database should contain:

- Protagonist
- Class/subclass
- Level / XP
- Captured monsters
- Monster Bond/Mastery
- Inventory
- Skills
- Equipment
- Story/world progress
- Adventure Points
- Momentum
- Calorie history
- Workout history
- Fitness synchronization/reward ledger
- Settings

Planned protection:

- Android automatic backup where appropriate
- Manual Export Save / Import Save
- Optional cloud-save support can be considered later

## Driving / False-Step Filtering

Health Connect step records do not inherently label each step as "walking" versus "while driving."

Use Android Activity Recognition as a supporting signal.

Supported useful states include:

- WALKING
- RUNNING
- ON_FOOT
- IN_VEHICLE
- ON_BICYCLE
- STILL

General rule:

- Strong walking/running evidence: count normally.
- Strong IN_VEHICLE evidence overlapping suspicious steps: hold/reject according to the final validation algorithm.
- Uncertain activity: prefer counting rather than aggressively deleting legitimate steps.

This is a single-player RPG. False rejection of real exercise is worse than occasionally allowing a small number of false steps.

Avoid continuous GPS solely for anti-cheat because of battery cost and false-positive complexity.

## Walking Rewards

Walking grants BOTH:

1. Adventure/Walking Points used for overworld exploration.
2. A modest amount of normal character XP.

Walking XP should be meaningful enough that a player stuck on a boss can continue walking over time, gain levels, and eventually become stronger.

Walking should not become the main or fastest source of leveling compared with actually playing the RPG.

Reward values and diminishing returns will be balanced later.

## Adventure / Overworld System

Real-world walking generates Adventure Points.

Adventure Points allow movement/exploration across an RPG overworld.

Possible map content:

- Normal encounters
- Treasure
- Camps
- Random events
- Dungeons
- Elite monsters
- NPCs
- Resource nodes
- Special discoveries

Walking should unlock opportunities rather than automatically defeating enemies.

The player still needs to play the RPG to win encounters.

## Momentum

Real-world activity can fill a Momentum resource.

Possible uses:

- Enter optional encounters
- Recover after defeat
- Empower an ultimate skill
- Reroll treasure
- Heal between battles
- Open special dungeon rooms
- Challenge elite monsters

Exact balance will be determined later.

## Main Character

The protagonist chooses an RPG class and later a subclass.

Exercise type does NOT determine or lock the character's class.

Possible initial direction:

- Warrior
- Rogue
- Mage
- Cleric

Possible subclasses will be designed later.

## Monster-Catching Party

The player is not a single-character party.

Current preferred structure:

- Protagonist
- Three captured monsters
- Four active party members total

The final active-party size can still be revisited, but four is currently preferred.

### Monster Levels

Captured monsters synchronize to the protagonist's current level.

Example:

A level 27 protagonist captures a new monster.

The monster joins at level 27 rather than level 1.

This prevents newly captured monsters from requiring a large catch-up grind.

### Monster Bond / Mastery

Separate from normal level, each monster develops a Bond/Mastery progression by actually being used.

Possible Bond/Mastery rewards:

- New skills
- Passive traits
- Alternative abilities
- Stat specializations
- Evolutions/forms
- Cosmetic changes

Normal protagonist progression raises the general power floor of the collection.

Using a specific monster develops that monster individually.

### Monster Roles

Monsters do not have conventional RPG classes.

Their species, stat profile, passives, and abilities naturally determine their roles.

Examples:

- Physical tank
- Healer/support
- Elemental glass cannon
- Debuff/status specialist
- Fast multi-hit attacker
- Defensive support
- Hybrid roles

Capture mechanics will be designed in detail later.

## Monster Discovery Through Activity

Potential direction:

Exercise/walking milestones can occasionally create Expeditions or discoveries.

Example:

"A strange trail has appeared."

The player can investigate using Adventure Points and potentially encounter unusual monsters or events.

Exercise should influence opportunities, not determine whether a player is allowed to own a particular combat archetype.

## Calorie / Food Log

The player can manually enter:

- Food name
- Calories
- Optional additional nutrition fields later

Example:

Burger — 650 calories

At daily rollover, archive that day's total.

Provide graph views such as:

- 7 days
- 30 days
- 90 days
- All time

The graph expands as new daily records are added.

The player can set a calorie target.

RPG benefits can be awarded for meeting the target, but the system must NOT reward increasingly extreme calorie restriction.

Possible model:

- At/below reasonable target: full bonus
- Slightly above: partial bonus
- Far above: no bonus
- Going dramatically below target does not generate larger and larger rewards

Health Connect nutrition integration can be considered/added where useful.

## Workout Logging

Manual workout logging remains important, particularly for strength training.

Possible detailed entry:

Bench Press
185 lb
3 sets
8 / 8 / 6 reps

The game should remember recent exercises and previous values so logging becomes fast.

Quick-session logging should also be possible:

- Strength training — 30 min
- Bodyweight — 20 min
- Cycling — 45 min
- Swimming — 30 min
- Other — 25 min

Compatible Health Connect workout data may be imported when available.

## Daily / Weekly Objectives

Possible daily objectives:

- Walk X steps
- Complete X minutes of exercise
- Meet calorie target
- Defeat X enemies
- Find a treasure room

Possible weekly objectives:

- Walk X total steps
- Exercise on X days
- Complete a dungeon
- Defeat a boss
- Meet calorie target on X of 7 days

Avoid punishing streak systems where missing one day destroys an enormous long-term streak.

## Initial Version Scope

A practical first version can include:

- Android fitness capability detection
- Health Connect integration where supported
- Direct/local step tracking
- Activity Recognition support
- Debug multi-counter step screen
- Character-creation fitness baseline
- Reliable step reconciliation/reward ledger
- Food/calorie log
- Daily calorie history
- Expanding line graph
- Manual workout logging
- Main character
- Initial class system
- Walking XP
- Adventure Points
- Basic Momentum
- Turn-based combat
- Captured monsters
- Level synchronization
- Bond/Mastery foundation
- Basic overworld/adventure system
- Local persistent save
- Backup/export foundation

## Still To Design

Major areas intentionally left open for later discussion:

- RPG interface/layout
- Combat UI
- Detailed combat rules
- Stats
- Exact classes/subclasses
- Skill systems
- Monster roster
- Monster capture procedure
- Bond/Mastery progression details
- Evolution/forms
- Story/world/theme
- Equipment
- Boss design
- Adventure Point conversion rate
- Walking XP balance
- Momentum balance
- Exact calorie rewards
- Exercise reward balance
- Visual identity
