package net.nekometa.pvpbot;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.config.ModConfigs;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.nekometa.pvpbot.ai.BotAiAttachments;
import net.nekometa.pvpbot.fight.FightAttachments;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(PvPBotMod.MODID)
public class PvPBotMod {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "pvpbot";
    // Holds the ModContainer reference for config persistence at runtime
    private static ModContainer modContainer;
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Items which will all be registered under the "pvpbot" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "pvpbot" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // PvP Bot設定GUIを開くアイテム(サインUIの代替)
    public static final DeferredItem<Item> PVPBOT_MENU_ITEM = ITEMS.register("pvpbot_menu",
            id -> new net.nekometa.pvpbot.PvpBotMenuItem(new Item.Properties()
                    .setId(net.minecraft.resources.ResourceKey.create(Registries.ITEM, id)).stacksTo(1)));

    // PvP Bot用クリエイティブタブ
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PVPBOT_TAB = CREATIVE_MODE_TABS.register("pvpbot_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.pvpbot"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> PVPBOT_MENU_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(PVPBOT_MENU_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public PvPBotMod(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (PvPBotMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register our attachment types (BotAiState等) so entities can hold them
        BotAiAttachments.register(modEventBus);
        FightAttachments.register(modEventBus);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        PvPBotMod.modContainer = modContainer;
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // --- Config persistence ---

    /** Save the COMMON config to disk. */
    public static void saveConfig() {
        if (modContainer == null) {
            return;
        }
        for (ModConfig config : ModConfigs.getModConfigs(modContainer.getModId())) {
            if (config.getType() == ModConfig.Type.COMMON
                    && config.getSpec() == Config.SPEC) {
                config.getLoadedConfig().save();
                break;
            }
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
        LOGGER.info("PvP Bot defaults: enemyArmor={}, playerArmor={}, boxing={}",
                Config.ENEMY_ARMOR_TIER.getAsInt(),
                Config.PLAYER_ARMOR_TIER.getAsInt(),
                Config.BOXING_MODE.getAsInt());
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }
}
