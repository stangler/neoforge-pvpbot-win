package net.nekometa.pvpbot.ai;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/**
 * NeoForge Data Attachment の登録。
 *
 * 元データパックはscoreboardという「エンティティに紐づく汎用データストレージ」を
 * 使っていたが、Java側では標準のフィールドを持てないバニラEntity(PiglinBrute等)に
 * 対して同じことをする必要があるため、Attachment APIで代替する。
 *
 * ※ NeoForge 26.1.2 時点のAttachment APIパッケージ・メソッドシグネチャは
 *   ビルド時に実際のJavadoc/生成ソースと突き合わせて確認すること。
 *   (バージョンアップで細部が変わっている可能性あり)
 */
public final class BotAiAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, "pvpbot");

    public static final Supplier<AttachmentType<BotAiState>> BOT_AI_STATE =
            ATTACHMENT_TYPES.register("bot_ai_state",
                    () -> AttachmentType.builder(BotAiState::new).build());

    private BotAiAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
