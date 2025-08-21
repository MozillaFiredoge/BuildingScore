package org.firedoge.models;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.firedoge.Main;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class Structure {
    private List<_BlockData> blocks;
    private String structureName;

    // TODO:
    // Add custom structure features

    public static Structure loadStructure(File file) throws IOException {
        Gson gson = new Gson();
        try (Reader reader = new FileReader(file)) {
            Structure structure = gson.fromJson(reader, Structure.class);
            String fileName = file.getName();
            if (fileName.endsWith(".json")) {
                fileName = fileName.substring(0, fileName.length() - 5);
            }
            structure.structureName = fileName;
            return structure;
        }
    }

    public void placeStructure(Location origin) {
        World world = origin.getWorld();
        for (_BlockData b : this.blocks) {
            Material mat = Material.valueOf(b.material);
            Block block = world.getBlockAt(origin.clone().add(b.x, b.y, b.z));
            block.setType(mat, false);

            if (b.blockDataString != null) {
                try {
                    BlockData data = Bukkit.createBlockData(b.blockDataString);
                    block.setBlockData(data, false);
                } catch (IllegalArgumentException e) {
                    Main.getPlugin().getLogger().log(Level.WARNING, "Invalid BlockData string: {0}", b.blockDataString);
                }
            }
        }
    }

    public void rotateStructure(int angle) {
        if (angle % 90 != 0) {
            throw new IllegalArgumentException("Angle must be a multiple of 90");
        }

        int rotations = (angle / 90) % 4;
        for (_BlockData b : this.blocks) {
            for (int i = 0; i < rotations; i++) {
                // rotate coords
                int temp = b.x;
                b.x = -b.z;
                b.z = temp;

                // rotate BlockData string if it has facing
                if (b.blockDataString != null) {
                    b.blockDataString = rotateBlockData(b.blockDataString);
                }
            }
        }
    }

    private String rotateBlockData(String data) {

        return data
                .replace("north", "TEMP")
                .replace("south", "north")
                .replace("TEMP", "south")
                .replace("east", "TEMP2")
                .replace("west", "east")
                .replace("TEMP2", "west");
    }

    public static List<Structure> loadAllStructuresFromResources() throws IOException {
        List<Structure> structures = new ArrayList<>();
        ClassLoader classLoader = Structure.class.getClassLoader();
        URL resource = classLoader.getResource("structures/");
        if (resource == null) {
            throw new IOException("Structures folder not found in resources");
        }

        try {
            // List all files in the structures folder
            JarURLConnection jarConnection = (JarURLConnection) resource.openConnection();
            JarFile jarFile = jarConnection.getJarFile();
            Enumeration<JarEntry> entries = jarFile.entries();

            Gson gson = new Gson();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith("structures/") && entry.getName().endsWith(".json")) {
                    try (InputStream fileStream = classLoader.getResourceAsStream(entry.getName());
                         Reader fileReader = new InputStreamReader(fileStream)) {
                        Structure structure = gson.fromJson(fileReader, Structure.class);
                        String fileName = entry.getName().substring("structures/".length());
                        if (fileName.endsWith(".json")) {
                            fileName = fileName.substring(0, fileName.length() - 5);
                        }
                        structure.structureName = fileName;
                        structures.add(structure);
                    }
                }
            }
        }catch (JsonIOException | JsonSyntaxException | IOException e) {
            Main.getPlugin().getLogger().log(Level.WARNING, "Error loading structure from resources", e);
        }

        return structures;
    }

    public String getStructureName() {
        return structureName;
    }
    public void setBlocks(List<_BlockData> blocks) {
        this.blocks = blocks;
    }

    public static class _BlockData {
        int x, y, z;
        String material;
        String blockDataString;

        public _BlockData(int x, int y, int z, String material, String blockDataString) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.material = material;
            this.blockDataString = blockDataString;
        }
    }
}