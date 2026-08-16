package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Central override layer applied to generated data right before it is written out.
 *
 * <p>Everything the generators emit is derived from the running game, which is normally the most
 * accurate source. A few values however are known to be wrong when read straight from the game
 * (or were historically wrong in minecraft-data and are now asserted by community-verified data).
 * Keeping those corrections here means regenerating any version always produces the corrected
 * output, instead of silently reverting manual fixes in the minecraft-data repository.</p>
 */
public final class DataOverrides {
    private DataOverrides() {
    }

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    public static void apply(String dataName, JsonElement output, String versionName) {
        if (!(output instanceof JsonArray array)) {
            return;
        }
        switch (dataName) {
            case "items" -> applyItems(array, versionName);
            case "blocks" -> applyBlocks(array, versionName);
            case "effects" -> applyEffects(array, versionName);
            default -> {
            }
        }
    }

    /**
     * Items that must always be reported with a fixed stack size. The game reports the registry
     * default, which for these items differs from the value the player can actually stack.
     * minecraft-data: PrismarineJS/minecraft-data#569 (warped fungus on a stick, 1.16-1.18).
     */
    private static final Map<String, Integer> ITEM_STACK_SIZE_OVERRIDES = Map.of(
            "warped_fungus_on_a_stick", 1
    );

    private static void applyItems(JsonArray items, String versionName) {
        if (!atLeast(versionName, "1.16") || !before(versionName, "1.19")) {
            return;
        }
        for (JsonElement element : items) {
            JsonObject item = element.getAsJsonObject();
            Integer stackSize = ITEM_STACK_SIZE_OVERRIDES.get(item.get("name").getAsString());
            if (stackSize != null) {
                item.addProperty("stackSize", stackSize);
            }
        }
    }

    private static void applyBlocks(JsonArray blocks, String versionName) {
        boolean waterAndLavaFix = atLeast(versionName, "1.17");
        boolean copperOreFix = atLeast(versionName, "1.18") && before(versionName, "1.19");
        if (!waterAndLavaFix && !copperOreFix) {
            return;
        }
        for (JsonElement element : blocks) {
            JsonObject block = element.getAsJsonObject();
            String name = block.get("name").getAsString();
            if (waterAndLavaFix && (name.equals("water") || name.equals("lava"))) {
                // Liquids report a high hardness but can never be mined.
                // minecraft-data: PrismarineJS/minecraft-data#926.
                block.addProperty("diggable", false);
            } else if (copperOreFix && name.equals("copper_ore")) {
                // minecraft-data: PrismarineJS/minecraft-data#538.
                block.addProperty("hardness", 3.0d);
                block.addProperty("resistance", 3.0d);
            }
        }
    }

    private static void applyEffects(JsonArray effects, String versionName) {
        if (!atLeast(versionName, "1.14.4") || !before(versionName, "1.17")) {
            return;
        }
        // Effect ids were shifted by one in the 1.14-1.16 era; assert the canonical ids and drop
        // any duplicate entries that may have crept into older data. minecraft-data:
        // PrismarineJS/minecraft-data#529.
        Map<String, Integer> canonicalIds = Map.of(
                "SlowFalling", 28,
                "ConduitPower", 29,
                "DolphinsGrace", 30,
                "BadOmen", 31,
                "HeroOfTheVillage", 32
        );
        Set<String> seenNames = new HashSet<>();
        for (int i = effects.size() - 1; i >= 0; i--) {
            JsonObject effect = effects.get(i).getAsJsonObject();
            String name = effect.get("name").getAsString();
            if (!seenNames.add(name)) {
                effects.remove(i);
            } else {
                Integer canonicalId = canonicalIds.get(name);
                if (canonicalId != null) {
                    effect.addProperty("id", canonicalId);
                }
            }
        }
    }

    private static boolean atLeast(String versionName, String minimum) {
        int[] version = toComponents(versionName);
        int[] min = toComponents(minimum);
        for (int i = 0; i < version.length; i++) {
            if (version[i] != min[i]) {
                return version[i] > min[i];
            }
        }
        return true;
    }

    private static boolean before(String versionName, String maximum) {
        int[] version = toComponents(versionName);
        int[] max = toComponents(maximum);
        for (int i = 0; i < version.length; i++) {
            if (version[i] != max[i]) {
                return version[i] < max[i];
            }
        }
        return false;
    }

    private static int[] toComponents(String versionName) {
        Matcher matcher = VERSION_PATTERN.matcher(versionName);
        if (!matcher.find()) {
            return new int[]{0, 0, 0};
        }
        return new int[]{
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) == null ? 0 : Integer.parseInt(matcher.group(2)),
                matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3))
        };
    }
}
