package me.rosillogames.eggwars.utils.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Decoder;
import com.mojang.serialization.JsonOps;
import me.rosillogames.eggwars.EggWars;

public class Reflections_1_20 implements Reflections
{
    private final String fl_1;
    private final String fl_2;
    private final String fl_3;

    public Reflections_1_20(boolean newV)
    {
        this.fl_1 = newV ? "dM" : "dJ";
        this.fl_2 = newV ? "ge" : "ga";
        this.fl_3 = newV ? "b" : "a";
    }

    @Override
    public void setArmorStandInvisible(ArmorStand stand)
    {
        stand.setInvisible(true);
    }

    @Override
    public void setTNTSource(TNTPrimed tnt, Player player)
    {
        tnt.setSource(player);
    }

    @Override
    public void setItemAge(Item item, int age) {
        try {
            Object obj = item.getClass().getMethod("getHandle").invoke(item);
            Field field = obj.getClass().getField("g");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            field.set(obj, age);
            field.setAccessible(accessible);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public ItemStack parseItemStack(JsonObject json) {
        try {
            Class cItemStack = this.getNMSClass("world.item.ItemStack");
            DataResult<Pair> result = ((Decoder)cItemStack.getField("a").get((Object)null)).decode(JsonOps.INSTANCE, json);
            HelpObject<ItemStack> helpstack = new HelpObject<ItemStack>();
            Class cCraftItemStack = this.getOBCClass("inventory.CraftItemStack");
            helpstack.object = new ItemStack(Material.AIR);
            result.resultOrPartial(s -> EggWars.instance.getLogger().log(Level.WARNING, s)).ifPresent((legacystack) ->
            {
                try {
                    helpstack.object = (ItemStack)cCraftItemStack.getMethod("asBukkitCopy", cItemStack).invoke((Object)null, legacystack.getFirst());
                } catch (Exception var5) {
                }

            });
            return helpstack.object;
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return new ItemStack(Material.AIR);
    }

    @Nullable
    @Override
    public BlockData parseBlockData(String string)
    {
        try
        {
            Class cMojangsonParser = this.getNMSClass("nbt.MojangsonParser");
            Object blockNbt = cMojangsonParser.getMethod("a", String.class).invoke(null, string);
            Class cNBTCompound = this.getNMSClass("nbt.NBTTagCompound");
            Class cGameProfileSerializer = this.getNMSClass("nbt.GameProfileSerializer");
            Class cHolderGetter = this.getNMSClass("core.HolderGetter");
            Class cBIR = this.getNMSClass("core.registries.BuiltInRegistries");
            Object registry = cBIR.getField("f").get((Object)null);
            Object holdLook = registry.getClass().getMethod("p").invoke(registry);
            Object blockData = cGameProfileSerializer.getMethod("a", cHolderGetter, cNBTCompound).invoke(null, holdLook, blockNbt);
            Class cIBlockData = this.getNMSClass("world.level.block.state.IBlockData");
            Class cCraftBlockData = this.getOBCClass("block.data.CraftBlockData");
            return (BlockData)cCraftBlockData.getMethod("fromData", cIBlockData).invoke(null, blockData);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void hideDyeFlag(LeatherArmorMeta meta)
    {
        meta.addItemFlags(ItemFlag.HIDE_DYE);
    }

    @Nullable
    @Override
    public String getStackName(ItemStack stack)
    {
        try
        {
            Class cItemStack = this.getNMSClass("world.item.ItemStack");
            Class cCraftItemStack = this.getOBCClass("inventory.CraftItemStack");
            Object nmsStack = cCraftItemStack.getMethod("asNMSCopy", stack.getClass()).invoke((Object)null, stack);
            Object nameComponent = cItemStack.getMethod("y").invoke(nmsStack);
            Class cIChatBase = this.getNMSClass("network.chat.IChatBaseComponent");
            Object string = cIChatBase.getMethod("getString").invoke(nameComponent);
            return (String)string;
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public List<String> getEnchantmentsLore(ItemStack stack)
    {
        List<String> list = new ArrayList();

        try
        {
            for (Map.Entry<Enchantment, Integer> entry : stack.getEnchantments().entrySet())
            {
                Class cCraftEnch = this.getOBCClass("enchantments.CraftEnchantment");
                Object nmsEnch = cCraftEnch.getMethod("getRaw", Enchantment.class).invoke(null, entry.getKey());
                Class cEnch = this.getNMSClass("world.item.enchantment.Enchantment");
                Object nameComponent = cEnch.getMethod("d", int.class).invoke(nmsEnch, entry.getValue());
                Class cIChatBase = this.getNMSClass("network.chat.IChatBaseComponent");
                Object string = cIChatBase.getMethod("getString").invoke(nameComponent);
                list.add("§7" + (String)string);
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return list;
    }

    @Override
    public void killOutOfWorld(Player p) {
        try {

        }
        catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void saveFullWorld(World world)
    {
        try
        {
            Object obj = world.getClass().getMethod("getHandle").invoke(world);
            Field field = obj.getClass().getField("e");
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            boolean boolVal = field.getBoolean(obj);
            field.setAccessible(accessible);
            obj.getClass().getMethod("a", this.getNMSClass("util.IProgressUpdate"), boolean.class, boolean.class).invoke(obj, null, true, false);
            field.setAccessible(true);
            field.set(obj, boolVal);
            field.setAccessible(accessible);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    @Nullable
    @Override
    public Block getEndChestBlock(Player p)
    {
        try
        {
            Object nmsP = p.getClass().getMethod("getHandle").invoke(p);
            Object nmsEC = nmsP.getClass().getMethod(this.fl_2).invoke(nmsP);
            Field field = nmsEC.getClass().getDeclaredFields()[0];
            boolean accessible = field.isAccessible();
            field.setAccessible(true);
            Object ecTE = field.get(nmsEC);
            field.setAccessible(accessible);

            if (ecTE != null)
            {
                Object blockPos = ecTE.getClass().getMethod("p").invoke(ecTE);
                int x = (int)blockPos.getClass().getMethod("u").invoke(blockPos);
                int y = (int)blockPos.getClass().getMethod("v").invoke(blockPos);
                int z = (int)blockPos.getClass().getMethod("w").invoke(blockPos);
                nmsEC.getClass().getMethod("c", this.getNMSClass("world.entity.player.EntityHuman")).invoke(nmsEC, nmsP);
                return p.getWorld().getBlockAt(x, y, z);
            }
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }

        return null;
    }

    @Override
    public void sendPacket(Player player, Object packetObj)
    {//as of 1.20.2 packet sender class is ServerCommonPacketListenerImpl
        try {
            Object handle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnection = handle.getClass().getField("b").get(handle);
            playerConnection.getClass().getMethod("a", getNMSClass("net.minecraft.network.protocol.Packet")).invoke(playerConnection, packetObj);
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }

    @Override
    public void sendTitle(Player player, Integer fadeInTime, Integer stayTime, Integer fadeOutTime, String title, String subtitle) {
        player.sendTitle(title, subtitle, fadeInTime, stayTime, fadeOutTime);
    }

    @Override
    public void sendActionBar(Player player, String s, Integer integer, Integer integer1, Integer integer2)
    {
        try
        {
            Class cTitleAnimPacket = this.getNMSClass("network.protocol.game.ClientboundSetTitlesAnimationPacket");
            Constructor constructor = cTitleAnimPacket.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
            Object packet = constructor.newInstance(integer, integer1, integer2);
            this.sendPacket(player, packet);

            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(s));
        }
        catch (Exception exception)
        {
            exception.printStackTrace();
        }
    }
}
