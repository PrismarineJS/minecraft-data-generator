package dev.u9g.minecraftdatagenerator;

import dev.u9g.minecraftdatagenerator.generators.DataGenerators;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.logging.Logger;

public class MinecraftDataGenerator {
    private static final Logger LOGGER = Logger.getLogger(MinecraftDataGenerator.class.getSimpleName());

    public static void start(String versionName, Path serverRootDirectory) {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out)));
        System.setErr(new PrintStream(new FileOutputStream(FileDescriptor.err)));
        MinecraftDataGenerator.LOGGER.info("Starting data generation for version %s".formatted(versionName));
        Path dataDumpDirectory = serverRootDirectory.resolve("minecraft-data").resolve(versionName);
        boolean success = DataGenerators.runDataGenerators(dataDumpDirectory, versionName);
        if (success) {
            MinecraftDataGenerator.LOGGER.info("Data generation successful!");
        } else {
            MinecraftDataGenerator.LOGGER.severe("Data generation failed!");
        }
        Runtime.getRuntime().halt(success ? 0 : 1);
    }
}
