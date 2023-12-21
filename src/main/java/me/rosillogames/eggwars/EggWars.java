package me.rosillogames.eggwars;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.rosillogames.eggwars.arena.Arena;
import me.rosillogames.eggwars.arena.SetupGUI;
import me.rosillogames.eggwars.commands.CmdEw;
import me.rosillogames.eggwars.commands.CmdLeave;
import me.rosillogames.eggwars.commands.CmdSetup;
import me.rosillogames.eggwars.database.Database;
import me.rosillogames.eggwars.dependencies.DependencyUtils;
import me.rosillogames.eggwars.enums.ArenaStatus;
import me.rosillogames.eggwars.enums.Versions;
import me.rosillogames.eggwars.language.LanguageManager;
import me.rosillogames.eggwars.listeners.*;
import me.rosillogames.eggwars.loaders.*;
import me.rosillogames.eggwars.objects.ArenaSign;
import me.rosillogames.eggwars.player.EwPlayer;
import me.rosillogames.eggwars.utils.*;
import me.rosillogames.eggwars.utils.reflection.ReflectionUtils;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;

public class EggWars extends JavaPlugin
{
    public static String EGGWARS_VERSION;
    public static EggWars instance;
    public static BungeeCord bungee = new BungeeCord();
    public static File arenasFolder;
    public static ConfigAccessor signsConfig;
    public static Set<EwPlayer> players;
    public static Set<ArenaSign> signs;
    public static Config config = new Config();
    public static Versions serverVersion;
    private ArenaLoader arenaLoader;
    private KitLoader kitLoader;
    private TokenLoader tokenLoader;
    private GeneratorLoader generatorLoader;
    private TradingLoader tradingLoader;
    private LanguageManager languageManager;
    private Database database;
    private Gson gson;

    public EggWars()
    {
    }

    @Override
    public void onDisable()
    {
        if (!serverVersion.isAllowedVersion())
        {
            return;
        }

        for (Arena arena : this.arenaLoader.getArenas())
        {
            arena.closeArena();
        }

        if (this.database != null)
        {
            this.database.savePlayers();
            this.database.close();
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        EGGWARS_VERSION = this.getDescription().getVersion();
        String s = this.getServer().getClass().getPackage().getName();
        serverVersion = Versions.get(s.substring(s.lastIndexOf('.') + 1));

        if (!serverVersion.isAllowedVersion()) {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[EggWars] " + ChatColor.RESET + "Incompatible version! Currently supported: " + Versions.SUPPORTED_TEXT);
            Bukkit.shutdown();
            return;
        }

        ReflectionUtils.setReflections(serverVersion);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.languageManager = new LanguageManager();
        this.gson = new Gson();
        this.loadLists();
        this.loadNamespaces();
        config.loadConfig();
        this.languageManager.loadLangs();
        this.loadFiles();
        this.eventRegister();
        this.commandRegister();
        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            DependencyUtils.registerEggWarsPlaceHolders();
            ChatUtils.placeholderAPI(true);
        }
        this.loadLoaders();
        this.loadArenas();
        this.loadSigns();
        TickClock.start();
        this.kitLoader.loadKits();
        this.tokenLoader.loadTokens();
        this.generatorLoader.loadGenerators();
        this.tradingLoader.loadTrades();

        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(player, null));
        }
        sendLogMessage("&7-----------------------------------");
        sendLogMessage(" ");
        sendLogMessage("&fServer: &c" + getServer().getName() + " " + getServer().getBukkitVersion());
        sendLogMessage("&fSuccessfully Plugin &aEnabled! &cv" + getDescription().getVersion());
        sendLogMessage("&fEditor: &dUnDeadlyDev");
        sendLogMessage(" ");
        sendLogMessage("&7-----------------------------------");

    }

    public void sendLogMessage(String msg) {
        Bukkit.getConsoleSender().sendMessage(parseLegacy("[EggWars] " + msg));
    }

    private void loadLoaders()
    {
        this.kitLoader = new KitLoader();
        this.arenaLoader = new ArenaLoader();
        this.tokenLoader = new TokenLoader();
        this.generatorLoader = new GeneratorLoader();
        this.tradingLoader = new TradingLoader();
        this.database = new Database(this);
    }

    private void loadNamespaces()
    {
        ItemUtils.genType = new NamespacedKey(this, "GEN_TYPE");
        ItemUtils.genLevel = new NamespacedKey(this, "GEN_LEVEL");
        ItemUtils.openMenu = new NamespacedKey(this, "OPEN_MENU");
    }

    private void loadLists()
    {
        Colorizer.init();
        players = new HashSet();
        signs = new HashSet();
    }

    private void eventRegister()
    {
        PluginManager pluginmanager = getServer().getPluginManager();
        pluginmanager.registerEvents(new BlockBreakListener(), this);
        pluginmanager.registerEvents(new BlockPlaceListener(), this);
        pluginmanager.registerEvents(new EggWarsListener(), this);
        pluginmanager.registerEvents(new EggInteractListener(), this);
        pluginmanager.registerEvents(new EntityExplodeListener(), this);
        pluginmanager.registerEvents(new EntityHurtListener(), this);
        pluginmanager.registerEvents(new EntitySpawnListener(), this);
        pluginmanager.registerEvents(new InventoryListener(), this);
        pluginmanager.registerEvents(new ItemPickupListener(), this);
        pluginmanager.registerEvents(new PlayerChatListener(), this);
        pluginmanager.registerEvents(new PlayerCraftListener(), this);
        pluginmanager.registerEvents(new PlayerDeathListener(), this);
        pluginmanager.registerEvents(new PlayerDropListener(), this);
        pluginmanager.registerEvents(new PlayerInteractListener(), this);
        pluginmanager.registerEvents(new PlayerJoinListener(), this);
        pluginmanager.registerEvents(new PlayerLeaveListener(), this);
        pluginmanager.registerEvents(new PlayerMoveListener(), this);
        pluginmanager.registerEvents(new ServerListPingListener(), this);
        pluginmanager.registerEvents(new SetupGUI.Listener(), this);
        pluginmanager.registerEvents(new SignChangeListener(), this);
        pluginmanager.registerEvents(new SignClickListener(), this);
    }

    private void commandRegister()
    {
        this.getCommand("ews").setExecutor(new CmdSetup());
        this.getCommand("ew").setExecutor(new CmdEw());
        this.getCommand("leave").setExecutor(new CmdLeave());
    }

    private void loadFiles()
    {
        arenasFolder = new File(this.getDataFolder(), "arenas");

        if (!arenasFolder.exists())
        {
            arenasFolder.mkdirs();
        }

        if (!arenasFolder.isDirectory())
        {
            arenasFolder.delete();
            arenasFolder.mkdirs();
        }
    }

    private void loadArenas()
    {
        if (!bungee.isEnabled())
        {
            File afile[] = arenasFolder.listFiles();
            int i = afile.length;

            for (int k = 0; k < i; k++)
            {
                File file = afile[k];

                if (!file.exists() || !file.isDirectory())
                {
                    continue;
                }

                if (!file.exists())
                {
                    file.mkdirs();
                }

                if (!file.isDirectory())
                {
                    file.delete();
                    file.mkdirs();
                }

                try
                {
                    this.arenaLoader.addArena(ArenaLoader.loadArena(file));
                }
                catch (Exception exception)
                {
                    exception.printStackTrace();
                }
            }
        }
        else
        {
            File afile1[] = arenasFolder.listFiles();

            if (bungee.useRandomArena() && afile1.length > 0)
            {
                this.loadRandomArena();
                return;
            }

            bungee.setArena(null);
            int j = afile1.length;
            int l = 0;

            do
            {
                if (l >= j)
                {
                    break;
                }

                File file1 = afile1[l];

                if (!file1.exists() || !file1.isDirectory())
                {
                    continue;
                }

                if (file1.getName().equals("Bungee"))
                {
                    Arena arena = ArenaLoader.loadArena(file1);
                    this.arenaLoader.addArena(arena);
                    bungee.setArena(arena);
                    break;
                }

                l++;
            }
            while (true);

            if (bungee.getArena() == null)
            {
                Arena arena1 = new Arena("Bungee");
                this.arenaLoader.addArena(arena1);
                arena1.setStatus(ArenaStatus.SETTING);
                arena1.getWorld().getBlockAt(0, 99, 0).setType(Material.STONE);
                bungee.setArena(arena1);
            }
        }
    }

    public void loadRandomArena()
    {
        List<File> list = Arrays.asList(arenasFolder.listFiles());
        List<File> list1 = new ArrayList();

        for (File file : list)
        {
            if (file.exists() && file.isDirectory())
            {
                list1.add(file);
            }
        }

        Collections.shuffle(list1);
        Arena arena = ArenaLoader.loadArena((File)list1.get(0));
        this.arenaLoader.addArena(arena);
        bungee.setArena(arena);
    }

    private void loadSigns()
    {
        if (bungee.isEnabled())
        {
            return;
        }

        signsConfig = new ConfigAccessor(this, new File(this.getDataFolder(), "signs.yml"));
        FileConfiguration fileconfiguration = signsConfig.getConfig();
        fileconfiguration.addDefault("Signs", new ArrayList());
        fileconfiguration.options().copyDefaults(true);
        signsConfig.saveConfig();

        for (String s : fileconfiguration.getStringList("Signs"))
        {
            Arena arena;
            Location location;

            try
            {
                JsonObject entryjson = GsonHelper.parse(s);
                JsonObject locjson = GsonHelper.getAsJsonObject(entryjson, "location");
                World world = Bukkit.getWorld(GsonHelper.getAsString(locjson, "world_name"));
                double d0 = (double)GsonHelper.getAsFloat(locjson, "x");
                double d1 = (double)GsonHelper.getAsFloat(locjson, "y");
                double d2 = (double)GsonHelper.getAsFloat(locjson, "z");
                location = new Location(world, d0, d1, d2);
                arena = this.arenaLoader.getArenaByName(GsonHelper.getAsString(entryjson, "arena"));
            }
            catch (Exception ex)
            {
                continue;
            }

            if (location.getWorld() != null && location.getBlock().getState() instanceof Sign && LobbySigns.isValidWallSign((Sign)location.getBlock().getState()))
            {
                signs.add(new ArenaSign(arena, location));
            }
        }

        saveSigns();
    }

    public static void saveSigns()
    {
        List list = new ArrayList();

        for (ArenaSign ewsign : EggWars.signs)
        {
            Location loc = ewsign.getLocation();
            JsonObject json = new JsonObject();
            json.addProperty("x", loc.getX());
            json.addProperty("y", loc.getY());
            json.addProperty("z", loc.getZ());
            json.addProperty("world_name", loc.getWorld().getName());
            JsonObject json1 = new JsonObject();
            json1.add("location", json);
            json1.addProperty("arena", ewsign.getArena().getName());
            list.add(json1.toString());
        }

        EggWars.signsConfig.createNewConfig();
        EggWars.signsConfig.getConfig().set("Signs", list);
        EggWars.signsConfig.saveConfig();
    }

    public Gson getGson()
    {
        return this.gson;
    }

    public static ArenaLoader getArenaManager()
    {
        return instance.arenaLoader;
    }

    public static KitLoader getKitManager()
    {
        return instance.kitLoader;
    }

    public static TokenLoader getTokenManager()
    {
        return instance.tokenLoader;
    }

    public static GeneratorLoader getGeneratorManager()
    {
        return instance.generatorLoader;
    }

    public static TradingLoader getTradingManager()
    {
        return instance.tradingLoader;
    }

    public static Database getDB()
    {
        return instance.database;
    }

    public static LanguageManager languageManager()
    {
        return instance.languageManager;
    }

    public void saveCustomResource(String resPath, File outFile, boolean warnIfExists)
    {
        if (resPath == null || resPath.equals(""))
        {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resPath = resPath.replace('\\', '/');
        InputStream in = this.getResource(resPath);

        if (in == null)
        {
            this.getLogger().log(Level.SEVERE, "The embedded resource '" + resPath + "' cannot be found in " + this.getFile());
            return;
        }

        int lastIndex = resPath.lastIndexOf('/');

        if (outFile == null)
        {
            File outDir = new File(this.getDataFolder(), resPath.substring(0, (lastIndex >= 0) ? lastIndex : 0));
            outFile = new File(outDir, resPath.contains("/") ? resPath.substring((lastIndex >= 0) ? lastIndex + 1 : 1, resPath.length()) : resPath);

            if (!outDir.exists())
            {
                outDir.mkdirs();
            }
        }

        try
        {
            if (!outFile.exists())
            {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;

                while ((len = in.read(buf)) > 0)
                {
                    out.write(buf, 0, len);
                }

                out.close();
                in.close();
            }
            else if (warnIfExists)
            {
                this.getLogger().log(Level.WARNING, "Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists");
            }
        }
        catch (IOException ex)
        {
            this.getLogger().log(Level.SEVERE, "Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    public static String parseLegacy(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

}
