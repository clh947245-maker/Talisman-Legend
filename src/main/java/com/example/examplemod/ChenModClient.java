package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 客户端模组主类 (ChenModClient)
 *
 * 这个类专门用于处理仅在客户端 (Client) 运行的逻辑。
 * 它被标记为 {@link Dist#CLIENT}，因此不会在专用服务端加载，这保证了访问客户端代码（如渲染、GUI）时的安全性。
 */
// @Mod 注解标记这是一个模组入口点，dist = Dist.CLIENT 表示只在客户端加载
@Mod(value = ChenMod.MODID, dist = Dist.CLIENT)
// @EventBusSubscriber 自动将此类中的静态 @SubscribeEvent 方法注册到模组事件总线 (Mod Event Bus)
@EventBusSubscriber(modid = ChenMod.MODID, value = Dist.CLIENT)
public class ChenModClient {

    /**
     * 构造函数
     *
     * 在模组加载早期被调用。这里主要用于注册配置屏幕工厂。
     *
     * @param container 模组容器实例  ..
     */
    public ChenModClient(ModContainer container) {
        // 允许 NeoForge 为此模组创建配置屏幕。
        // 玩家可以通过 "模组" 菜单 -> 选择本模组 -> 点击 "配置" 来访问。
        // 注意：需要确保 en_us.json 文件中有对应的翻译键。
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * 事件监听：客户端设置 (FMLClientSetupEvent)
     *
     * 在客户端初始化阶段触发。用于执行需要 Minecraft 客户端实例完全准备好后的任务。
     * 例如：注册按键绑定、注册实体渲染器、注册方块颜色处理器等。
     *
     * @param event 客户端设置事件
     */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // 示例：打印日志，证明客户端代码正在运行
        ChenMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        ChenMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
