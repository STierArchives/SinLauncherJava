// App.java

package com.example.SinLauncher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.example.SinLauncher.SinLauncherEntites.User;

import com.example.SinLauncher.SinLauncherEntites.Arch;
import com.example.SinLauncher.SinLauncherEntites.Instance;
import com.example.SinLauncher.SinLauncherEntites.Os;
import com.example.SinLauncher.SinLauncherEntites.Instance.InstanceAlreadyExistsException;
import com.example.SinLauncher.SinLauncherEntites.Instance.InvalidInstanceVersionException;
import com.example.SinLauncher.config.Config;
import com.example.SinLauncher.config.Java;
import com.example.SinLauncher.json.Accounts;
import com.example.SinLauncher.json.Client;
import com.example.SinLauncher.json.Manifest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

public class App {
    public static final Logger LOGGER = Logger.getLogger(App.class.getName());
    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Client.Argument.class, new Client.ArgumentDeserializer()).create();

    public static final String DIR;
    public static final Path ASSETS_DIR;
    public static final Path LIBRARIES_DIR;
    public static final Path NATIVES_DIR;

    public static final Os OS;
    public static final Arch ARCH;

    public static Config CONFIG;

    static User user = new User("0", "IliaSigma960", "12345678", "Haxxers@gmail.com", true, true);

    static {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            DIR = System.getenv("APPDATA") + "\\SinLauncher";
            OS = Os.Windows;
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            DIR = System.getProperty("user.home") + "/.sinlauncher";
            OS = Os.Linux;
        }
        else {
            DIR = "SinLauncher";
            OS = Os.Linux;
        }

        String arch = System.getProperty("os.arch").toLowerCase();

        switch (arch) {
            case "x86":
                ARCH = Arch.X86;
                break;
            case "arm":
                ARCH = Arch.Arm;
                break;
            case "aarch64":
                ARCH = Arch.Arm64;
                break;
            default:
                ARCH = Arch.X86_64;
                break;
        }

        ASSETS_DIR = Paths.get(DIR, "assets");
        LIBRARIES_DIR = Paths.get(DIR, "libraries");
        NATIVES_DIR = Paths.get(DIR, "natives");

        try {
            App.initialize();
            CONFIG = Config.readMainConfig();
        }
        catch (IOException e) {
            LOGGER.info("Failed to initialize the Launcher. ERROR: " + e.getMessage());
            System.exit(1);
        }
    }

    static void initializeInstances() throws IOException {
        if (!Files.exists(Instance.PARENT_DIR))
            Files.createDirectories(Instance.PARENT_DIR);

        if (!Files.exists(Instance.INSTANCES_FILE)) {
            Path file = Files.createFile(Instance.INSTANCES_FILE);

            Files.writeString(file, "[]");
        }
    }

    static void initializeLauncherDir() throws IOException {
        Path dir = Paths.get(DIR);

        if (!Files.exists(dir))
            Files.createDirectories(dir);

        if (!Files.exists(ASSETS_DIR))
            Files.createDirectories(ASSETS_DIR);

        if (!Files.exists(LIBRARIES_DIR))
            Files.createDirectories(LIBRARIES_DIR);

        App.initializeInstances();

        HttpResponse<String> response = Unirest
                .get("https://launchermeta.mojang.com/mc/game/version_manifest.json")
                .asString();

        if (response.getStatus() == 200)
            Files.write(Manifest.PATH, response.getBody().getBytes());
        else {
            if (!Files.exists(Manifest.PATH))
                throw new IOException("Failed to fetch Manifest.JSON; Response code: " + response.getStatus());
        }
    }

    public static void initialize() throws IOException {
        initializeLauncherDir();

        LOGGER.info("Launcher initialized!");
    }

    public static void installationManager(String installationName, String version, Java[] cups, int _cups_arg) throws IOException {
        try {
            Instance.createInstance(installationName, version);

            Instance createdInstance = Instance.getInstance(installationName);

            SwingUtilities.invokeLater(() -> {
                int dialogResult = JOptionPane.showConfirmDialog(null,
                        "Warning: 1GB of data is about to be installed! Do you want to continue?",
                        "Confirm Installation", JOptionPane.YES_NO_OPTION);

                if (dialogResult == JOptionPane.YES_OPTION) {
                    try {
                        LOGGER.info("Starting installation for instance: " + installationName);

                        if (createdInstance != null) {
                            createdInstance.install();
                        }

                        LOGGER.info("Installation completed for instance: " + installationName);

                        JOptionPane.showMessageDialog(null,
                                "Installation is complete!",
                                "Installation Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Installation error: ", e);
                    }
                } else {
                    JOptionPane
                            .showMessageDialog(
                                    null,
                                    "Installation has been cancelled!",
                                    "Installation Cancelled",
                                    JOptionPane.INFORMATION_MESSAGE);
                }
            });

        } catch (InstanceAlreadyExistsException e) {
            LOGGER.info("Instance already exists: " + e.getMessage());

            SwingUtilities.invokeLater(() -> {
                Instance existingInstance = Instance.getInstance(installationName);

                if (existingInstance != null) {
                    try {
                        if (_cups_arg < 0 || _cups_arg >= cups.length) {
                            LOGGER.severe("Invalid index for Java cups: " + _cups_arg);

                            return;
                        }

                        LOGGER.info("Setting Java configuration and launching instance: " + installationName);

                        existingInstance.getConfig().setJava(cups[_cups_arg]);
                        existingInstance.install();
                        existingInstance.launch();
                    } catch (IOException __e) {
                        LOGGER.log(Level.SEVERE, "Launch error: ", __e);
                    }
                } else {
                    LOGGER.warning("Failed to retrieve the instance for launching.");
                }
            });

        } catch (InvalidInstanceVersionException e) {
            LOGGER.log(Level.SEVERE, "Invalid instance version: ", e);
        }
    }

    public static void main(String[] args) {
        try {
            Manifest manifest = Manifest.readManifest();

            System.out.println("Max Ram: " + CONFIG.getMaxRam());
            System.out.println("Latest Minecraft Version: " + manifest.latest.release);

            var cups = Java.getAvailableJavaCups();

            Accounts.addUser(user);
            Accounts.readAccounts().getUser(user.getUsername());
            
            CONFIG.setUsername(user.getUsername());

            installationManager("newInstallationName2", "1.21.1", cups, 0);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Exception: ", e);
        }
    }
}
