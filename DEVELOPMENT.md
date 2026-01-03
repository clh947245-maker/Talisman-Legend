# 符咒与魔法系统开发文档

本文档详细说明了 ChenMod 中符咒（Talisman）与魔法（Magic）系统的开发规范、架构设计以及新增符咒的流程。所有新增的符咒和魔法效果必须遵循此文档。

## 1. 核心设计理念

本模组的符咒系统基于以下核心机制：
1.  **物品触发**：玩家手持符咒物品右键点击触发效果。
2.  **MobEffect 实现**：所有的魔法能力本质上都是 Minecraft 的 `MobEffect`（药水效果）。
3.  **背包检测与依赖**：
    *   符咒效果虽然是持续的，但**必须依赖背包中的符咒物品**。
    *   系统会实时监测玩家背包（包括主手、副手、物品栏）。
    *   一旦检测到玩家拥有某符咒的效果但背包中没有对应的符咒物品，效果持续时间会被强制缩短至 **0.5秒**（10 ticks），从而实现“丢弃符咒即失效”的逻辑。
4.  **独立参数**：每个符咒的持续时间和冷却时间是独立的，可在各自的物品类中单独定义。

---

## 2. 关键常量定义

所有符咒应遵循以下数值标准（可在各符咒类中统一定义或引用）：

*   **魔法持续时间 (MAGIC_DURATION)**: 建议默认 `600` ticks (30秒 / 半分钟)，可根据需求调整。
*   **冷却时间 (COOLDOWN_TICKS)**: 建议默认 `20` ticks (1秒)，可根据需求调整。

---

## 3. 开发流程指南

### 步骤 1: 创建魔法效果类 (Magic Effect)

在 `com.example.examplemod.magic` 包下创建新的魔法类，继承自 `MobEffect`。

**模板代码 (`XxxPowerMagic.java`):**

```java
package com.example.examplemod.magic;

import com.example.examplemod.ChenMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
// 如果需要属性修饰符 (AttributeModifier) 导入相关类

/**
 * 魔法效果："XXX的力量"
 */
public class XxxPowerMagic extends MobEffect {

    // 如果需要属性修饰符，在此定义 UUID 和 数值常量
    // public static final ResourceLocation MODIFIER_ID = ...

    /**
     * API: 赋予实体XXX力量效果 (指定持续时间)
     */
    public static void grantXxxPower(LivingEntity entity, int duration) {
        if (entity == null) return;
        
        // 统一参数：visible=true (显示粒子), showIcon=true (显示图标)
        entity.addEffect(new MobEffectInstance(
            ChenMod.XXX_POWER, 
            duration, 
            0, 
            true, 
            true, 
            true
        ));
    }

    public XxxPowerMagic() {
        // BENEFICIAL (有益), 颜色 (十六进制)
        super(MobEffectCategory.BENEFICIAL, 0xFFFFFF);
        
        // 如果是属性类效果（如增加攻击力），在此处添加 addAttributeModifier
    }

    // 如果是逻辑类效果（如每tick回血），重写下面两个方法
    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // 每 tick 执行
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        // 实现具体逻辑
        return true;
    }
}
```

### 步骤 2: 创建符咒物品类 (Talisman Item)

在 `com.example.examplemod.talisman` 包下创建新的符咒物品类，继承自 `Item`。

**模板代码 (`XxxTalismanItem.java`):**

```java
package com.example.examplemod.talisman;

import com.example.examplemod.ChenMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public class XxxTalismanItem extends Item {

    // 魔法效果持续时间常量 (半分钟)
    public static final int MAGIC_DURATION = 600;
    // 冷却时间常量 (1秒)
    public static final int COOLDOWN_TICKS = 20;

    public XxxTalismanItem() {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack itemStack = player.getItemInHand(usedHand);

        if (!level.isClientSide) {
            // 给予魔法效果
            player.addEffect(new MobEffectInstance(ChenMod.XXX_POWER, MAGIC_DURATION, 0, true, true, true));
            
            // 添加冷却时间
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }

        return InteractionResultHolder.success(itemStack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("item.chen_mod.xxx_talisman.desc"));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
```

### 步骤 3: 注册 (Registration)

在 `com.example.examplemod.ChenMod` 类中进行注册：

1.  **注册物品**:
    ```java
    public static final DeferredItem<XxxTalismanItem> XXX_TALISMAN = ITEMS.register("xxx_talisman", XxxTalismanItem::new);
    ```

2.  **注册魔法效果**:
    ```java
    public static final DeferredHolder<MobEffect, XxxPowerMagic> XXX_POWER = MOB_EFFECTS.register("xxx_power", XxxPowerMagic::new);
    ```

3.  **添加到创造模式物品栏**:
    在 `addCreative` 方法中添加 `event.accept(XXX_TALISMAN);`。

### 步骤 4: 背包检测逻辑 (Inventory Check)

这是**至关重要**的一步。必须在 `com.example.examplemod.event.TalismanInventoryHandler` 类中注册新的检测逻辑。

**修改 `onPlayerTick` 方法：**

```java
@SubscribeEvent
public static void onPlayerTick(PlayerTickEvent.Post event) {
    // ... 前置检查 ...

    // 检测 XXX 符咒
    if (player.hasEffect(ChenMod.XXX_POWER)) {
        checkTalismanInInventory(player, ChenMod.XXX_POWER, ChenMod.XXX_TALISMAN.get());
    }
}
```

### 步骤 5: 资源文件 (Resources)

1.  **语言文件 (Lang)**:
    *   `src/main/resources/assets/chen_mod/lang/zh_cn.json`
    *   `src/main/resources/assets/chen_mod/lang/en_us.json`
    *   添加 Item 名称、Item 描述 (`.desc`) 和 Effect 名称。

2.  **纹理 (Textures)**:
    *   **符咒物品图标**: `src/main/resources/assets/chen_mod/textures/item/xxx_talisman.png`
    *   **魔法效果图标**: `src/main/resources/assets/chen_mod/textures/mob_effect/xxx_power.png` (通常复制符咒图标即可)

---

## 4. 架构图示

```mermaid
graph TD
    Player[玩家] -->|右键点击| Item[符咒物品 (XxxTalismanItem)]
    Item -->|给予| Effect[魔法效果 (XxxPowerMagic)]
    Item -->|触发| Cooldown[冷却时间 (2400 ticks)]
    
    subgraph Server Tick Loop
        Handler[TalismanInventoryHandler] -->|监听| PlayerTick[PlayerTickEvent]
        Handler -->|检查| HasEffect{拥有效果?}
        HasEffect -- 是 --> CheckInv{背包有符咒?}
        CheckInv -- 否 --> ReduceDuration[缩短持续时间至 0.5s]
        CheckInv -- 是 --> KeepDuration[保持正常持续时间]
    end
```
