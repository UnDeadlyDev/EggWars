package me.rosillogames.eggwars.loaders;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import com.google.common.collect.Sets;
import me.rosillogames.eggwars.EggWars;
import me.rosillogames.eggwars.arena.Arena;
import me.rosillogames.eggwars.enums.MenuType;
import me.rosillogames.eggwars.language.TranslationUtils;
import me.rosillogames.eggwars.player.inventory.TranslatableItem;
import me.rosillogames.eggwars.utils.ItemUtils;

public class ArenaLoader
{
    public static final char[] REPLACE_FILE_CHARS = new char[] {' ', '/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
    private static TranslatableItem leaveItem;
    private Set<Arena> arenas = Sets.<Arena>newHashSet();

    public ArenaLoader()
    {
    }

    public static void loadConfig() {
        ItemStack stack = ItemUtils.getItemOrDefault(EggWars.instance.getConfig().getString("inventory.leave.item"), Material.RED_BED);
        ItemUtils.hideStackAttributes(stack);
        ItemUtils.addCustomModelData(stack, Integer.valueOf(EggWars.instance.getConfig().getInt("inventory.leave.modelData")));
        ItemUtils.setOpensMenu(stack, MenuType.LEAVE_ARENA);
        leaveItem = TranslatableItem.translatableNameLore(stack, "gameplay.leave.item_lore", "gameplay.leave.item_name");

    }

    public static ItemStack getLeaveItem(Player player) {
        return leaveItem.getTranslated(player);
    }

    public static Arena loadArena(File arenaFolderIn)
    {
        return new Arena(arenaFolderIn, null);
    }

    public static String getValidArenaID(String name)
    {
        for (char c0 : REPLACE_FILE_CHARS)
        {
            name = name.replace(c0, '_');
        }

        name = name.replaceAll("[./\"]", "_");

        if (name.length() > 255)
        {
            name = name.substring(0, 255);
        }

        return name.toLowerCase();
    }

    public static String formulateName(String[] args, int beginIdx)
    {
        StringBuilder sb = new StringBuilder("");

        for (int i = beginIdx; i < args.length; ++i)
        {
            sb.append(args[i]).append(" ");
        }

        return sb.toString().trim();
    }

    public Set<Arena> getArenas()
    {
        return new HashSet(this.arenas);
    }

    public boolean addArena(Arena arena)
    {
        return this.arenas.add(arena);
    }

    public boolean removeArena(Arena arena)
    {
        return this.arenas.remove(arena);
    }

    public Arena getArenaByName(String s)
    {
        for (Arena arena : this.getArenas())
        {
            if (arena.getName().equalsIgnoreCase(s))
            {
                return arena;
            }
        }

        return null;
    }

    public Arena getArenaById(String s)
    {
        for (Arena arena : this.getArenas())
        {
            if (arena.getId().equalsIgnoreCase(s))
            {
                return arena;
            }
        }

        return null;
    }

    public Arena getArenaByWorld(World world)
    {
        for (Arena arena : this.getArenas())
        {
            if (arena.getWorld() == world)
            {
                return arena;
            }
        }

        return null;
    }

    public Arena cmdArenaByIdOrName(CommandSender sender, String[] s, int beginIdx)
    {
        String name = formulateName(s, beginIdx);
        Arena arena;

        if ((arena = this.getArenaByName(name)) != null)
        {
            return arena;
        }

        if ((arena = this.getArenaById(s[beginIdx])) != null)
        {
            return arena;
        }

        TranslationUtils.sendMessage("commands.error.arena_does_not_exist", sender, name);
        return null;
    }
}
