package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.registry.Registry;

public class SoundsDataGenerator implements IDataGenerator {
    public static JsonObject generateSound(SoundEvent soundEvent) {
        JsonObject soundDesc = new JsonObject();

        soundDesc.addProperty("id", Registry.SOUND_EVENT.getRawId(soundEvent)); // raw 0-indexed sound_event registry id. The wire packet encodes (id + 1),
        // with 0 meaning "read an inline SoundEvent instead" — but that offset is applied by
        // the packet codec (registryEntryHolder), NOT here. Adding it to the registry dump
        // double-counts and shifts every consumer's sound lookup by one.
        soundDesc.addProperty("name", soundEvent.getId().getPath());

        return soundDesc;
    }

    @Override
    public String getDataName() {
        return "sounds";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry.SOUND_EVENT.forEach(sound -> resultsArray.add(generateSound(sound)));
        return resultsArray;
    }
}
