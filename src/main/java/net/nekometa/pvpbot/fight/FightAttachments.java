package net.nekometa.pvpbot.fight;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class FightAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "pvpbot");

    public static final Supplier<AttachmentType<FightSession>> FIGHT_SESSION =
            ATTACHMENT_TYPES.register("fight_session",
                    () -> AttachmentType.builder(FightSession::new).build());

    private FightAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
