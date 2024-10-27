package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.NoteBlockAccessor;
import net.minecraft.sound.Sound;

import java.util.Objects;

public class InstrumentsDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "instruments";
    }

    @Override
    public JsonElement generateDataJson() {
        JsonArray array = new JsonArray();
        int i = 0;
        for (Sound sound : Objects.requireNonNull(NoteBlockAccessor.TUNES())) {
            JsonObject object = new JsonObject();
            object.addProperty("id", i++);
            object.addProperty("name", sound.getId().getPath().split("\\.")[2]);
            array.add(object);
        }
        return array;
    }
}
