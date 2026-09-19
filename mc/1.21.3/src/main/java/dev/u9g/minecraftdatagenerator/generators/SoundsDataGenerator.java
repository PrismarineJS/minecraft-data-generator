package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvent;

public class SoundsDataGenerator implements IDataGenerator {
    public static JsonObject generateSound(SoundEvent soundEvent) {
        JsonObject soundDesc = new JsonObject();

        soundDesc.addProperty("id", Registries.SOUND_EVENT.getRawId(soundEvent)); // the registry id: node-minecraft-protocol turns the packet value (id + 1, 0 = inline) back into it
        soundDesc.addProperty("name", soundEvent.id().getPath());

        return soundDesc;
    }

    @Override
    public String getDataName() {
        return "sounds";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registries.SOUND_EVENT.forEach(sound -> resultsArray.add(generateSound(sound)));
        return resultsArray;
    }
}
