package justs_js.cel;

import justs_js.cel.client.ClientEntitiesController;
import justs_js.cel.client.api.ClientEntity;
import justs_js.cel.client.impl.ClientEntityImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class CELModLib implements ClientModInitializer {
	public static final String MOD_ID = "cel";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static final ClientEntitiesController controller = new ClientEntitiesController();

	@Override
	public void onInitializeClient() {
		ClientTickEvents.START_LEVEL_TICK.register(controller::tick);
		LOGGER.info("Client Entities Lib loaded.");

		//testImpl(); // todo: remove
	}

	private void testImpl() {
		EntityType<ClientEntityImpl> CLIENT_ENTITY_IMPL = controller.register(
				"client_entity_impl",
				EntityType.Builder
						.of(ClientEntityImpl::new, MobCategory.MISC)
						.sized(0.6F, 1.95F)
						.eyeHeight(1.62F)
		);

		ClientCommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess) -> {
					dispatcher.register(
							literal(MOD_ID).then(
									literal("summon").executes(
											commandContext -> {
												ClientEntityImpl entity = new ClientEntityImpl(
														CLIENT_ENTITY_IMPL,
														commandContext.getSource().getLevel()
												);
												entity.snapTo(commandContext.getSource().getPosition());
												controller.addEntity(entity);
												return 1;
											}
									)
							)
					);

					dispatcher.register(
							literal(MOD_ID).then(
									literal("target").executes(
											commandContext -> {
												controller.forEach((e) -> ((ClientEntity)e).getBrain().setMemory(MemoryModuleType.AVOID_TARGET, commandContext.getSource().getPlayer()));
												return 1;
											}
									)
							)
					);

					dispatcher.register(
							literal(MOD_ID).then(
									literal("untarget").executes(
											commandContext -> {
												controller.forEach((e) -> ((ClientEntity)e).getBrain().eraseMemory(MemoryModuleType.AVOID_TARGET));
												return 1;
											}
									)
							)
					);
				}
		);

		FabricDefaultAttributeRegistry.register(CLIENT_ENTITY_IMPL, ClientEntityImpl.createAttributes());
		EntityRenderers.register(CLIENT_ENTITY_IMPL, NoopRenderer::new);
	}
}