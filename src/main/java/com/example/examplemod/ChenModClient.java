package com.example.examplemod;

import com.example.examplemod.client.ClientModEvents;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端模组主类 (ChenModClient)
 *
 * 这个类专门用于处理仅在客户端 (Client) 运行的逻辑。
 * 它被标记为 {@link Dist#CLIENT}，因此不会在专用服务端加载，这保证了访问客户端代码（如渲染、GUI）时的安全性。
 */
// @Mod 注解标记这是一个模组入口点，dist = Dist.CLIENT 表示只在客户端加载
public class ChenModClient {

    /**
     * 构造函数
     *
     * 在模组加载早期被调用。这里主要用于注册配置屏幕工厂。
     *
     * @param modEventBus 模组容器实例  ..
     */
    public ChenModClient(IEventBus modEventBus) {
        modEventBus.addListener(ClientModEvents::registerRenderers);
        modEventBus.addListener(ClientModEvents::registerKeyMappings);
        modEventBus.addListener(ClientModEvents::onAddLayers);
        modEventBus.addListener(ChenModClient::onClientSetup);
    }

    /**
     * 事件监听：客户端设置 (FMLClientSetupEvent)
     *
     * 在客户端初始化阶段触发。用于执行需要 Minecraft 客户端实例完全准备好后的任务。
     * 例如：注册按键绑定、注册实体渲染器、注册方块颜色处理器等。
     *
     * @param event 客户端设置事件
     */
    static void onClientSetup(FMLClientSetupEvent event) {
        // 示例：打印日志，证明客户端代码正在运行
        ChenMod.LOGGER.info("HELLO FROM CLIENT SETUP");
        ChenMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}
