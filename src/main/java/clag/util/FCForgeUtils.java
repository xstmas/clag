package clag.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.UUID;

public class FCForgeUtils {

    public static EntityPlayerMP getPlayer(String playerName){
        playerName = playerName.toLowerCase();
        for (EntityPlayerMP entityPlayer : (List<EntityPlayerMP>) MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (entityPlayer.getGameProfile().getName().toLowerCase().equals(playerName)){
                return entityPlayer;
            }
        }
        return null;
    }

    public static EntityPlayerMP getPlayer(UUID playerUUID){
        for (EntityPlayerMP entityPlayer : (List<EntityPlayerMP>) MinecraftServer.getServer().getConfigurationManager().playerEntityList) {
            if (entityPlayer.getGameProfile().getId().equals(playerUUID)){
                return entityPlayer;
            }
        }
        return null;
    }

    public static boolean isSinglePlayer(){
        return MinecraftServer.getServer().getEntityWorld().isRemote;
    }

}
