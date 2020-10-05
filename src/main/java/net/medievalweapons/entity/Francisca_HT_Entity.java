package net.medievalweapons.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.medievalweapons.item.Francisca_HT_Item;
import net.medievalweapons.network.EntitySpawnPacket;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Packet;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Francisca_HT_Entity extends PersistentProjectileEntity {
  private static final TrackedData<Boolean> ENCHANTMENT_GLINT;
  private ItemStack francisca_HT;
  private final Set<UUID> piercedEntities = new HashSet<>();

  public Francisca_HT_Entity(EntityType<? extends Francisca_HT_Entity> entityType, World world,
      Francisca_HT_Item item) {
    super(entityType, world);
    this.francisca_HT = new ItemStack(item);
  }

  public Francisca_HT_Entity(World world, LivingEntity owner, Francisca_HT_Item item, ItemStack stack) {
    super(item.getType(), owner, world);
    this.francisca_HT = new ItemStack(item);
    this.francisca_HT = stack.copy();
    this.dataTracker.set(ENCHANTMENT_GLINT, stack.hasGlint());
  }

  @Environment(EnvType.CLIENT)
  public Francisca_HT_Entity(World world, double x, double y, double z, Francisca_HT_Item item) {
    super(item.getType(), x, y, z, world);
    this.francisca_HT = new ItemStack(item);
  }

  @Override
  protected void initDataTracker() {
    super.initDataTracker();
    this.dataTracker.startTracking(ENCHANTMENT_GLINT, false);
  }

  @Override
  public Packet<?> createSpawnPacket() {
    return EntitySpawnPacket.createPacket(this);
  }

  @Override
  protected ItemStack asItemStack() {
    return this.francisca_HT.copy();
  }

  @Environment(EnvType.CLIENT)
  public boolean enchantingGlint() {
    return this.dataTracker.get(ENCHANTMENT_GLINT);
  }

  @Override
  protected void onEntityHit(EntityHitResult entityHitResult) {
    int level = EnchantmentHelper.getLevel(Enchantments.PIERCING, this.francisca_HT);
    Entity hitEntity = entityHitResult.getEntity();
    if (this.piercedEntities.contains(hitEntity.getUuid()) || this.piercedEntities.size() > level) {
      return;
    }
    this.piercedEntities.add(hitEntity.getUuid());
    float damage = ((Francisca_HT_Item) this.francisca_HT.getItem()).getAttackDamage() * 2;
    if (hitEntity instanceof AnimalEntity) {
      int impalingLevel = EnchantmentHelper.getLevel(Enchantments.IMPALING, this.francisca_HT);
      if (impalingLevel > 0) {
        damage += impalingLevel * 1.5F;
      }
    }

    Entity owner = this.getOwner();
    DamageSource damageSource = createDamageSource(this, owner == null ? this : owner);
    // SoundEvent soundEvent = CampanionSoundEvents.SPEAR_HIT_FLESH;
    if (hitEntity.damage(damageSource, damage)) {
      if (hitEntity.getType() == EntityType.ENDERMAN) {
        return;
      }

      if (hitEntity instanceof LivingEntity) {
        LivingEntity hitLivingEntity = (LivingEntity) hitEntity;
        if (owner instanceof LivingEntity) {
          EnchantmentHelper.onUserDamaged(hitLivingEntity, owner);
          EnchantmentHelper.onTargetDamaged((LivingEntity) owner, hitLivingEntity);
        }

        this.onHit(hitLivingEntity);
      }
    }

    if (this.piercedEntities.size() > level) {
      this.setVelocity(this.getVelocity().multiply(-0.01D, -0.1D, -0.01D));
    } else {
      this.setVelocity(this.getVelocity().multiply(0.75));
    }
    // this.playSound(soundEvent, 1.0F, 1.0F);
  }

  // @Override
  // protected SoundEvent getHitSound() {
  // return CampanionSoundEvents.SPEAR_HIT_GROUND;
  // }

  @Override
  public void onPlayerCollision(PlayerEntity player) {
    Entity entity = this.getOwner();
    if (entity == null || entity.getUuid() == player.getUuid()) {
      super.onPlayerCollision(player);
    }
  }

  @Override
  public void readCustomDataFromTag(CompoundTag tag) {
    super.readCustomDataFromTag(tag);
    if (tag.contains("francisca_ht", 10)) {
      this.francisca_HT = ItemStack.fromTag(tag.getCompound("francisca_ht"));
      this.dataTracker.set(ENCHANTMENT_GLINT, this.francisca_HT.hasGlint());
    }

    this.piercedEntities.clear();
    if (tag.contains("francisca_ht_hit", 9)) {
      for (Tag hitEntity : tag.getList("francisca_ht_hit", 10)) {
        this.piercedEntities.add(((CompoundTag) hitEntity).getUuid("UUID"));
      }
    }
  }

  @Override
  public void writeCustomDataToTag(CompoundTag tag) {
    super.writeCustomDataToTag(tag);
    tag.put("francisca_ht", this.francisca_HT.toTag(new CompoundTag()));

    ListTag tags = new ListTag();
    for (UUID uuid : this.piercedEntities) {
      CompoundTag c = new CompoundTag();
      c.putUuid("UUID", uuid);
      tags.add(c);
    }
    tag.put("francisca_ht_hit", tags);
  }

  @Override
  public void age() {
    if (this.pickupType != PersistentProjectileEntity.PickupPermission.ALLOWED) {
      super.age();
    }
  }

  @Override
  @Environment(EnvType.CLIENT)
  public boolean shouldRender(double cameraX, double cameraY, double cameraZ) {
    return true;
  }

  static {
    ENCHANTMENT_GLINT = DataTracker.registerData(Francisca_HT_Entity.class, TrackedDataHandlerRegistry.BOOLEAN);
  }

  public static DamageSource createDamageSource(Entity entity, Entity owner) {
    return new ProjectileDamageSource("francisca_ht", entity, owner).setProjectile();
  }

}