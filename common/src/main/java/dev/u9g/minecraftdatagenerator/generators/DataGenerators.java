package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DataGenerators {
    private static final List<IDataGenerator> GENERATORS = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(DataGenerators.class.getSimpleName());

    static {
        List<Class<IDataGenerator>> generators;
        try (ScanResult scanResult = new ClassGraph()
                .acceptPackages("dev.u9g.minecraftdatagenerator.generators")
                .overrideClassLoaders(IDataGenerator.class.getClassLoader())
                .enableClassInfo().scan()) {
            generators = scanResult
                    .getClassesImplementing(IDataGenerator.class)
                    .loadClasses(IDataGenerator.class);
        }

        for (Class<IDataGenerator> generatorClass : generators) {
            try {
                GENERATORS.add(generatorClass.getDeclaredConstructor().newInstance());
            } catch (ReflectiveOperationException e) {
                LOGGER.log(Level.SEVERE, "Failed to instantiate data generator %s".formatted(generatorClass.getName()), e);
            }
        }
    }

    public static boolean runDataGenerators(Path outputDirectory, String versionName) {
        try {
            Files.createDirectories(outputDirectory);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to create data generator output directory at %s".formatted(outputDirectory), e);
            return false;
        }

        int generatorsFailed = 0;
        LOGGER.info("Running minecraft data generators, output at %s".formatted(outputDirectory));

        for (IDataGenerator dataGenerator : GENERATORS) {
            if (!dataGenerator.isEnabled()) {
                LOGGER.info("Skipping disabled generator %s".formatted(dataGenerator.getDataName()));
                continue;
            }

            LOGGER.info("Running generator %s".formatted(dataGenerator.getDataName()));
            try {
                String outputFileName = "%s.json".formatted(dataGenerator.getDataName());
                JsonElement outputElement = dataGenerator.generateDataJson();
                DataOverrides.apply(dataGenerator.getDataName(), outputElement, versionName);
                Path outputFilePath = outputDirectory.resolve(outputFileName);

                try (Writer writer = Files.newBufferedWriter(outputFilePath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                    JsonWriter jsonWriter = new JsonWriter(writer);
                    jsonWriter.setIndent("  ");
                    Streams.write(outputElement, jsonWriter);
                }

                LOGGER.info("Generator: %s -> %s".formatted(dataGenerator.getDataName(), outputFileName));
            } catch (Throwable e) {
                LOGGER.log(Level.SEVERE, "Failed to run data generator %s".formatted(dataGenerator.getDataName()), e);
                generatorsFailed++;
            }
        }

        LOGGER.info("Finishing running data generators");
        return generatorsFailed == 0;
    }
}
