package com.example.examplemod.client.renderer;

import com.example.examplemod.network.SheepDisguiseState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class SheepDisguisePlayerRenderer extends PlayerRenderer {
    public SheepDisguisePlayerRenderer(EntityRendererProvider.Context context, boolean slim) {
        super(context, slim);
    }

    @Override
    public ResourceLocation getTextureLocation(AbstractClientPlayer player) {
        UUID skinSourceUUID = SheepDisguiseState.getSkinSource(player.getUUID());
        if (skinSourceUUID == null) {
            return super.getTextureLocation(player);
        }

        PlayerInfo playerInfo = Minecraft.getInstance().getConnection() == null
                ? null
                : Minecraft.getInstance().getConnection().getPlayerInfo(skinSourceUUID);
        if (playerInfo != null) {
            return playerInfo.getSkin().texture();
        }

        return DefaultPlayerSkin.get(skinSourceUUID).texture();
    }
}
