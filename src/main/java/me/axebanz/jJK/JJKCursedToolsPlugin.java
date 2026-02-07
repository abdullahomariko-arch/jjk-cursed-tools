package me.axebanz.jJK;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class JJKCursedToolsPlugin extends JavaPlugin {

    private static JJKCursedToolsPlugin instance;

    private ConfigManager configManager;
    private PlayerDataStore playerDataStore;

    private TechniqueRegistry techniqueRegistry;
    private TechniqueManager techniqueManager;

    private CursedEnergyManager cursedEnergyManager;
    private CooldownManager cooldownManager;
    private RegenLockManager regenLockManager;
    private NullifyManager nullifyManager;

    private ItemIds itemIds;
    private CursedToolFactory cursedToolFactory;

    private ActionbarUI actionbarUI;
    private BossbarUI bossbarUI;

    private AbilityService abilityService;

    private CommandRouter commandRouter;

    // ===== Divine Wheel =====
    private WheelUI wheelUI;
    private WheelAdaptationManager wheelManager;

    public static JJKCursedToolsPlugin get() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        configManager.load();

        this.playerDataStore = new PlayerDataStore(this);

        this.techniqueRegistry = new TechniqueRegistry();
        registerTechniques();
        this.techniqueManager = new TechniqueManager(this, techniqueRegistry, playerDataStore);

        this.cooldownManager = new CooldownManager(this, playerDataStore);
        this.regenLockManager = new RegenLockManager(this, playerDataStore);
        this.nullifyManager = new NullifyManager(this, techniqueManager, playerDataStore);

        this.cursedEnergyManager = new CursedEnergyManager(this, playerDataStore);

        this.itemIds = new ItemIds(this);
        this.cursedToolFactory = new CursedToolFactory(this, itemIds);

        this.actionbarUI = new ActionbarUI(this);
        this.bossbarUI = new BossbarUI(this);

        this.abilityService = new AbilityService(
                this,
                configManager,
                techniqueManager,
                cursedEnergyManager,
                cooldownManager,
                regenLockManager,
                nullifyManager,
                cursedToolFactory,
                actionbarUI,
                bossbarUI
        );

        // ===== Divine Wheel =====
        this.wheelUI = new WheelUI(this);
        this.wheelManager = new WheelAdaptationManager(this, wheelUI, playerDataStore, cursedToolFactory);

        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(this, playerDataStore, cursedEnergyManager, bossbarUI, actionbarUI), this);
        Bukkit.getPluginManager().registerEvents(new ToolUseListener(this, abilityService, cursedToolFactory), this);
        Bukkit.getPluginManager().registerEvents(new CombatListener(this, abilityService, cursedToolFactory, regenLockManager, nullifyManager), this);

        this.commandRouter = new CommandRouter(this);
        commandRouter.registerDefaults();
        getCommand("jjk").setExecutor(commandRouter);
        getCommand("jjk").setTabCompleter(commandRouter);

        CmdWheel wheelCmd = new CmdWheel(this, wheelManager, wheelUI);
        getCommand("wheel").setExecutor(wheelCmd);
        getCommand("wheel").setTabCompleter(wheelCmd);

        actionbarUI.start();
        bossbarUI.start();
        cursedEnergyManager.startRegenTask();

        wheelManager.start();

        Bukkit.getOnlinePlayers().forEach(p -> {
            playerDataStore.load(p.getUniqueId());
            cursedEnergyManager.ensureInitialized(p.getUniqueId());
            bossbarUI.attachPlayer(p);
        });

        getLogger().info("JJKCursedTools enabled.");
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            bossbarUI.detachPlayer(p);
            actionbarUI.clear(p.getUniqueId());
            playerDataStore.save(p.getUniqueId());
        });

        getLogger().info("JJKCursedTools disabled.");
    }

    public ConfigManager cfg() { return configManager; }
    public PlayerDataStore data() { return playerDataStore; }

    public TechniqueRegistry techniques() { return techniqueRegistry; }
    public TechniqueManager techniqueManager() { return techniqueManager; }

    public CursedEnergyManager ce() { return cursedEnergyManager; }
    public CooldownManager cooldowns() { return cooldownManager; }
    public RegenLockManager regenLock() { return regenLockManager; }
    public NullifyManager nullify() { return nullifyManager; }

    public ItemIds itemIds() { return itemIds; }
    public CursedToolFactory tools() { return cursedToolFactory; }

    public ActionbarUI actionbarUI() { return actionbarUI; }
    public BossbarUI bossbarUI() { return bossbarUI; }

    public AbilityService abilityService() { return abilityService; }
    public CommandRouter router() { return commandRouter; }

    public WheelAdaptationManager wheel() { return wheelManager; }
    public WheelUI wheelUI() { return wheelUI; }

    public void reloadAll() {
        configManager.load();
        Bukkit.getOnlinePlayers().forEach(p -> bossbarUI.attachPlayer(p));
    }

    private void registerTechniques() {
        techniqueRegistry.register(new GravityTechnique(this));
    }
}
