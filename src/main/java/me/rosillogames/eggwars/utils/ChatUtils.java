package me.rosillogames.eggwars.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;

public class ChatUtils {

    private static boolean placeholderAPI = false;

    public static void placeholderAPI(boolean api) {
        placeholderAPI = api;
    }

    public static String replace(String string, Player p){
        if(string == null){
            return "";
        }
        String newString = string;
        if (placeholderAPI) {
            newString = PlaceholderAPI.setPlaceholders(p, newString);
        }
        newString = HexUtils.colorify(newString);
        return newString;
    }
}

