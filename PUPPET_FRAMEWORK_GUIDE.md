# Developer Guide: Boss Puppet Framework Integration

This guide explains how to add **Fractured Utils** as a mod dependency and implement the **Boss Puppet Framework** (`IPuppetEntity` + `PuppetController`) in your custom Minecraft entities.

---

## 1. Overview

The **Boss Puppet Framework** provides a modular AI hijacking and action dispatch system. It uses a **Composition Pattern** to allow custom mobs and third-party entities to yield control to music sequencers and orchestrator scripts without forcing modifications to your mob class inheritance hierarchy.

---

## 2. Adding Fractured Utils as a Dependency

### A. Gradle Setup (`build.gradle`)

Add Fractured Utils to your `repositories` and `dependencies` blocks in `build.gradle`:

```groovy
repositories {
    // Maven repository hosting Fractured Utils (or local maven repository)
    maven {
        name = "Local Maven"
        url = "file://${project.projectDir}/mcmodsrepo"
    }
    // Alternatively, use CurseMaven if hosted on CurseForge:
    // maven { url = "https://www.cursemaven.com" }
}

dependencies {
    // Compile against Fractured Utils API
    implementation fg.deobf("net.dandare21.fracturedutils:fractured_utils:${fractured_utils_version}")
    
    // Or via CurseMaven:
    // implementation fg.deobf("curse.maven:fractured-utils-PROJECTID:FILEID")
}
```

### B. Mod Manifest (`META-INF/mods.toml`)

Declare Fractured Utils as a dependency in your `mods.toml`:

```toml
[[dependencies.your_mod_id]]
    modId = "fractured_utils"
    mandatory = true
    versionRange = "[1.0.0,)"
    ordering = "AFTER"
    side = "BOTH"
```

---

## 3. Architecture & Core Components

| Component | Class / Interface | Purpose |
| :--- | :--- | :--- |
| **Interface** | [`IPuppetEntity`](file:///d:/Projects/mc%20modding/Fractured%20Utils/src/main/java/net/dandare21/fracturedutils/puppet/IPuppetEntity.java) | Implemented by any `Mob` to allow puppeteering. |
| **Controller** | [`PuppetController`](file:///d:/Projects/mc%20modding/Fractured%20Utils/src/main/java/net/dandare21/fracturedutils/puppet/PuppetController.java) | Persistent entity component managing actions, lifetimes, and AI aspect suppression. |
| **Action Callback** | [`PuppetAction`](file:///d:/Projects/mc%20modding/Fractured%20Utils/src/main/java/net/dandare21/fracturedutils/puppet/PuppetAction.java) | Functional interface (`(Mob mob, CompoundTag params) -> void`) defining attack/movement routines. |
| **Registry** | [`PuppetActionRegistry`](file:///d:/Projects/mc%20modding/Fractured%20Utils/src/main/java/net/dandare21/fracturedutils/puppet/PuppetActionRegistry.java) | Builder helper used to map `ResourceLocation` action IDs to `PuppetAction` callbacks. |
| **Hijack Goal** | [`PuppetOverrideGoal`](file:///d:/Projects/mc%20modding/Fractured%20Utils/src/main/java/net/dandare21/fracturedutils/puppet/PuppetOverrideGoal.java) | Priority 0 AI goal (`MOVE`, `LOOK`, `TARGET`, `JUMP`) that silences standard AI goals while active. |

---

## 4. Entity Integration Step-by-Step

To make your custom entity puppet-controllable:

1. Implement `IPuppetEntity`.
2. Instantiate a `PuppetController` inside your entity.
3. Pass `PuppetActionRegistry` to `registerPuppetActions(registry)` during constructor initialization.
4. Forward entity tick calls to `puppetController.tick()`.

### Full Code Example

```java
package com.example.mymod.entity;

import net.dandare21.fracturedutils.puppet.IPuppetEntity;
import net.dandare21.fracturedutils.puppet.PuppetActionRegistry;
import net.dandare21.fracturedutils.puppet.PuppetController;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public class BossDemonEntity extends Monster implements IPuppetEntity {
    // 1. Instantiate the PuppetController component
    private final PuppetController puppetController = new PuppetController(this);

    public BossDemonEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        // 2. Register puppet actions during initialization
        this.registerPuppetActions(new PuppetActionRegistry(this.puppetController));
    }

    @Override
    public PuppetController getPuppetController() {
        return this.puppetController;
    }

    @Override
    public void registerPuppetActions(PuppetActionRegistry registry) {
        // Attack Action 1: Radial Shockwave Blast
        registry.register(ResourceLocation.fromNamespaceAndPath("mymod", "radial_blast"), (mob, params) -> {
            float radius = params.contains("radius") ? params.getFloat("radius") : 5.0f;
            // Suppress navigation & targeting during blast animation
            this.puppetController.setSuppressNavigation(true);
            this.puppetController.setSuppressTargeting(true);
            
            this.castRadialBlast(radius);
        });

        // Attack Action 2: Targeted Charge / Dash
        registry.register(ResourceLocation.fromNamespaceAndPath("mymod", "charge_player"), (mob, params) -> {
            double speed = params.contains("speed") ? params.getDouble("speed") : 2.0;
            LivingEntity target = this.getTarget();
            if (target != null) {
                this.puppetController.forceLookAt(target);
                this.puppetController.forceMoveTo(target.getX(), target.getY(), target.getZ(), speed);
            }
        });
    }

    private void castRadialBlast(float radius) {
        // Custom particle/damage shockwave logic here
    }

    @Override
    public void tick() {
        super.tick();
        // 3. Forward tick update to controller to manage action timeouts & callbacks
        this.puppetController.tick();
    }
}
```

---

## 5. Controlling Puppets Programmatically

### A. AI Suppression Controls

While puppeting is active, you can selectively toggle individual AI aspects:

```java
PuppetController controller = puppetEntity.getPuppetController();

// Halt pathfinding & navigation
controller.setSuppressNavigation(true);

// Clear current target entity
controller.setSuppressTargeting(true);

// Freeze head/body look controls
controller.setSuppressLook(true);

// Reset all suppression flags back to default (false)
controller.resetSuppressionFlags();
```

### B. Primitive Direct Controls

Direct primitives for sequencer scripts:

```java
// Direct entity pathfinding movement
controller.forceMoveTo(x, y, z, speed);

// Direct entity look angle
controller.forceLookAt(targetEntity);
controller.forceLookAt(x, y, z);
```

### C. Executing Scripted Actions

To trigger registered actions programmatically:

```java
CompoundTag params = new CompoundTag();
params.putFloat("radius", 8.0f);

// Execute action for 40 ticks (2 seconds) with a completion callback
controller.executeAction(
    ResourceLocation.fromNamespaceAndPath("mymod", "radial_blast"),
    params,
    40, // Duration in ticks
    () -> System.out.println("Radial blast completed!")
);
```

---

## 6. Orchestrator Sequence Integration

In Fractured Utils orchestrator sequences, you can invoke puppet actions directly via JSON sequence scripts:

```json
{
  "type": "puppet_action",
  "actionId": "mymod:radial_blast",
  "entityUuid": "12345678-1234-1234-1234-123456789abc",
  "durationTicks": 40
}
```

If `entityUuid` is left blank, Fractured Utils will dispatch the action to active `IPuppetEntity` instances in the active server level.
