package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.FieldHelper;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.WaterCreatureEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.Objects;

public class EntitiesDataGenerator implements IDataGenerator {
    public static JsonObject generateEntity(Registry<EntityType<?>> entityRegistry, EntityType<?> entityType) {
        JsonObject entityDesc = new JsonObject();
        Identifier registryKey = entityRegistry.getId(entityType);
        int entityRawId = entityRegistry.getRawId(entityType);

        entityDesc.addProperty("id", entityRawId);
        entityDesc.addProperty("internalId", entityRawId);
        entityDesc.addProperty("name", Objects.requireNonNull(registryKey).getPath());

        entityDesc.addProperty("displayName", DGU.translateText(entityType.getTranslationKey()));
        entityDesc.addProperty("width", entityType.getWidth());
        entityDesc.addProperty("height", entityType.getHeight());

        Entity entityObject = entityType.create(DGU.getWorld());
        String entityTypeString = entityObject != null ? getEntityTypeForClass(entityObject.getClass()) : "player";
        entityDesc.addProperty("type", entityTypeString);
        entityDesc.addProperty("category", getCategoryFrom(entityType));

        return entityDesc;
    }

    private static String getCategoryFrom(@NotNull EntityType<?> entityType) {
        ParameterizedType entityTypeClass = (ParameterizedType) FieldHelper.findStaticFieldWithValue(EntityType.class, entityType).getGenericType();
        Class<?> entityClass = (Class<?>) entityTypeClass.getActualTypeArguments()[0];
        return switch (entityClass.getPackageName()) {
            case "net.minecraft.entity.decoration", "net.minecraft.entity.decoration.painting" -> "Immobile";
            case "net.minecraft.entity.boss", "net.minecraft.entity.mob", "net.minecraft.entity.boss.dragon" ->
                    "Hostile mobs";
            case "net.minecraft.entity.projectile", "net.minecraft.entity.thrown" -> "Projectiles";
            case "net.minecraft.entity.passive" -> "Passive mobs";
            case "net.minecraft.entity.vehicle" -> "Vehicles";
            case "net.minecraft.entity.player", "net.minecraft.entity" -> "other";
            default -> throw new IllegalStateException("Unexpected entity type: " + entityClass.getPackageName());
        };
    }

    //Honestly, both "type" and "category" fields in the schema and examples do not contain any useful information
    //Since category is optional, I will just leave it out, and for type I will assume general entity classification
    //by the Entity class hierarchy (which has some weirdness too by the way)
    private static String getEntityTypeForClass(Class<? extends Entity> entityClass) {
        //Top-level classifications
        if (WaterCreatureEntity.class.isAssignableFrom(entityClass)) {
            return "water_creature";
        }
        if (AnimalEntity.class.isAssignableFrom(entityClass)) {
            return "animal";
        }
        if (HostileEntity.class.isAssignableFrom(entityClass)) {
            return "hostile";
        }
        if (AmbientEntity.class.isAssignableFrom(entityClass)) {
            return "ambient";
        }

        //Second level classifications. PathAwareEntity is not included because it
        //doesn't really make much sense to categorize by it
        if (PassiveEntity.class.isAssignableFrom(entityClass)) {
            return "passive";
        }
        if (MobEntity.class.isAssignableFrom(entityClass)) {
            return "mob";
        }

        //Other classifications only include living entities and projectiles. everything else is categorized as other
        if (LivingEntity.class.isAssignableFrom(entityClass)) {
            return "living";
        }
        if (ProjectileEntity.class.isAssignableFrom(entityClass)) {
            return "projectile";
        }
        return "other";
    }

    @Override
    public String getDataName() {
        return "entities";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultArray = new JsonArray();
        Registry<EntityType<?>> entityTypeRegistry = Registry.ENTITY_TYPE;
        entityTypeRegistry.forEach(entity -> resultArray.add(generateEntity(entityTypeRegistry, entity)));
        return resultArray;
    }
}
