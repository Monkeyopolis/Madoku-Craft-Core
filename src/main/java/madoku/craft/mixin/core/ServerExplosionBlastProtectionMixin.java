package madoku.craft.mixin.core;

import madoku.craft.core.enchant.EnchantBooksManager;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Applies configured Blast Protection knockback resistance without mob explosion behavior. */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionBlastProtectionMixin {
	@Redirect(
		method = "hurtEntities",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/entity/LivingEntity;getAttributeValue(Lnet/minecraft/core/Holder;)D"
		)
	)
	private double madokuCraft$applyBlastProtectionKnockbackResistance(
		LivingEntity entity,
		Holder<Attribute> attribute
	) {
		double currentResistance = entity.getAttributeValue(attribute);
		if (attribute == Attributes.EXPLOSION_KNOCKBACK_RESISTANCE) {
			return EnchantBooksManager.resolveExplosionKnockbackResistance(entity, currentResistance);
		}
		return currentResistance;
	}
}
