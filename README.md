# Chen Mod: Talisman Legends

Chen Mod: Talisman Legends is a Minecraft magic-adventure mod inspired by talisman powers, shadow magic, palace exploration, and animated boss encounters. The mod adds a full set of magical talismans, transformation gameplay, custom projectiles, Shadow Corps mechanics, new creatures, palace structures, loot integration, and utility items for testing or building.

The mod is built around the idea that magic should feel active: most powers are triggered by items, supported by custom effects, and tied to the player keeping the relevant talisman in their inventory.

## Supported Versions

This repository is organized with separate branches for different loader and Minecraft targets:

| Branch | Loader | Minecraft |
| --- | --- | --- |
| `forge-1.21.0` | Forge | 1.21 |
| `forge-1.21.1` | Forge | 1.21.1 |
| `neoforge-1.21.0` | NeoForge | 1.21 |
| `neoforge-1.21.1` | NeoForge | 1.21.1 |

Use the branch and jar that match your game installation. Jars built from one loader or Minecraft version should not be mixed into another environment.

## Main Features

- Twelve talisman-themed powers with custom items, effects, cooldowns, and inventory checks.
- Monkey Talisman transformation system with many animal forms.
- Tiger Talisman clone splitting and merging gameplay.
- Sheep Talisman astral projection with body tracking and return mechanics.
- Dragon, Mouse, Pig, and pufferfish magic projectiles.
- Ninja Mask and Shadow Corps command system.
- Shengzhu boss/entity content and Shadow Ninja enemies.
- Aibo, Mo Di Cai, and Aibo + Mo Di Cai Fusion entities.
- Sheng Zhu Palace worldgen structure and palace-related detection tools.
- Palace building constructor items for placing bundled structure templates.
- Custom loot modifiers that distribute talismans through vanilla exploration.
- Bilingual resource support through Minecraft language files.

## Talismans

### Horse Talisman

The Horse Talisman represents healing and purification. It is designed to expel harmful external forces from the player and provides the Horse Power effect.

### Ox Talisman

The Ox Talisman grants raw strength. It is themed around unstoppable physical force and provides the Ox Power effect.

### Rabbit Talisman

The Rabbit Talisman grants speed and mobility. It is especially useful when combined with movement-oriented powers.

### Snake Talisman

The Snake Talisman grants invisibility-style gameplay. Hostile mobs lose aggression toward the player while the power is active.

### Dog Talisman

The Dog Talisman focuses on survival and loyalty. It ties into dog/wolf themed mechanics and protective magic.

### Rooster Talisman

The Rooster Talisman grants levitation. Hold Shift while airborne to descend, and combine it with the Rabbit Talisman for stronger aerial movement.

### Monkey Talisman

The Monkey Talisman allows shapeshifting. Hold Tab and scroll to select a form, then use the talisman to transform.

Available forms include:

- Chicken
- Cow
- Pig
- Horse
- Wolf
- Cat
- Allay
- Bat
- Parrot
- Bee
- Cod
- Dolphin
- Turtle
- Tadpole
- Frog
- Squid
- Fox
- Sniffer
- Llama
- Rabbit
- Panda
- Pufferfish

The menu also includes a revert option to return to the original player form.

### Tiger Talisman

The Tiger Talisman represents the balance of Yin and Yang. It can split the player into an original body and a dark-side clone, with each side holding one half of the talisman. The two halves can resonate, track each other, and merge back together.

### Dragon Talisman

The Dragon Talisman fires a Dragon Blast forward. The projectile creates an explosive magical impact when it hits.

### Mouse Talisman

The Mouse Talisman fires a small beam that stops when it hits a block or entity. It is also tied to living-block style magic.

### Pig Talisman

The Pig Talisman fires twin yellow eye lasers. These lasers can damage creatures and break blocks along their path.

### Sheep Talisman

The Sheep Talisman enables astral projection. The player's soul can separate from the body to explore freely, while body tracking and return prompts help the player locate and rejoin the original body.

## Shadow Corps

The Ninja Mask gives the player access to Shadow Corps command gameplay. By default, the keybinds are:

| Key | Action |
| --- | --- |
| `X` | Summon Shadow Ninjas |
| `C` | Dismiss Shadow Ninjas |
| `B` | Command Shadow Corps to kneel |

The mod also adds Shadow Ninja entities, a Shadow General's Blessing effect, and Shadow Unmasking potion support.

## Uncle's Pufferfish

Uncle's Pufferfish is a magical weapon with two modes:

- Attack Mode: fires a pufferfish laser that damages enemies.
- Demonic Qi Sense Mode: helps detect the direction and distance of nearby Sheng Zhu Palace magic.

Right-click to switch modes. When sense mode is active, the item can glow when the player is facing detected demonic qi.

## Structures and Building Constructors

The mod adds Sheng Zhu Palace worldgen and a collection of constructor items for placing palace-related structures manually. These are useful for testing, map building, and quickly previewing structure pieces.

Included constructor variants:

- Chengtian Hall
- Qiyue Palace
- Lingxiao Tower
- Tingfeng Pavilion
- Tingyu Pavilion
- Lingyun Terrace
- Yingxia Waterside
- Huifeng Corridor
- Fuguang Boat
- Fengming Gate Tower
- Chonghua Gate
- Hanxiang Courtyard
- Mingde Hall
- Tingzhu Studio

Use a constructor in the air for remote placement, or use it on a block to place the structure at the target.

## Entities

The mod adds several custom entities:

- Shadow Corps - Ninja
- Shengzhu
- Aibo
- Mo Di Cai
- Aibo + Mo Di Cai Fusion
- Tiger Clone
- Sheep Body
- Dragon Blast
- Mouse Beam
- Pig Laser
- Pufferfish Laser
- Living Block

Some entities have spawn eggs available through the Chen Mod creative tab.

## Loot and Exploration

Talismans are integrated into exploration through custom loot modifiers. Different talismans are associated with different vanilla activities or structures, including fishing, bastions, ocean ruins, dungeons, ancient cities, end cities, nether fortresses, turtle-related loot, and other adventure targets.

This encourages players to discover talismans naturally instead of receiving every power immediately.

## Installation

1. Install the correct Minecraft loader for your target branch: Forge or NeoForge.
2. Use the jar built for the same Minecraft version as your game.
3. Place the jar in your `.minecraft/mods` folder or the instance-specific `mods` folder.
4. Install required dependencies if your chosen jar does not bundle them.

Forge 1.21.1 builds may produce an `-all.jar` artifact that bundles Geckolib through JarJar. If both a normal jar and an `-all.jar` are available for that version, use the `-all.jar` when you do not want to install Geckolib separately.

## Building From Source

Clone the repository, check out the branch for the version you want, then build with Gradle:

```powershell
git checkout forge-1.21.1
.\gradlew.bat build
```

If ForgeGradle certificate checks fail in your environment, run:

```powershell
.\gradlew.bat "-Dnet.minecraftforge.gradle.check.certs=false" build
```

Built jars are generated in:

```text
build/libs/
```

## Development Notes

- Mod id: `chen_mod`
- Main creative tab: `Chen Mod`
- Java target: Java 21 for Minecraft 1.21+
- Geckolib is used for animated entity/model support.
- Talisman powers are generally implemented as item-triggered effects.
- Several powers depend on server/client networking for selection menus, projectiles, body tracking, clone state, and weapon actions.
- When adding a new talisman, remember to add the item, effect, inventory validation, language keys, textures, and creative tab entry.

## License

All Rights Reserved unless another license is explicitly provided by the project owner.
