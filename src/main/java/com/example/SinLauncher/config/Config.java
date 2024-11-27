// Config.java

package com.example.SinLauncher.config;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.springframework.context.annotation.Configuration;

import com.example.SinLauncher.App;
import com.example.SinLauncher.SinLauncherEntites.Instance;
import com.example.SinLauncher.SinLauncherEntites.Os;
import com.example.SinLauncher.SinLauncherEntites.User;
import com.example.SinLauncher.json.Accounts;
import com.example.SinLauncher.json.Client;
import com.example.SinLauncher.json.Accounts.NoSuchAccountException;
import com.sun.management.OperatingSystemMXBean;

@Configuration
public class Config {
    public static final Path MAIN_PATH = Paths.get(App.DIR, "config.json");

    // String so Gson doesnt cry :(
    private String path;
    private long min_ram;
    private long max_ram;
    private Java java;
    private User user;

    /* Creates a new null config at path */
    public Config(Path path) throws IOException {
        this.path = path.toString();
        this.max_ram = 0;
        this.min_ram = 0;
        this.java = null;
        this.user = null;

        this.writeConfig();
    }

    /* Retrieves a Config from path */
    public static Config getConfig(Path path) throws IOException {
        Config config = App.GSON.fromJson(Files.readString(path), Config.class);
        config.path = path.toString();

        return config;
    }

    /** The default config */
    private Config() throws IOException {
        OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long total = os.getTotalMemorySize();

        this.max_ram = total / 4 / 1024 / 1024;
        this.min_ram = this.max_ram / 2;

        this.java = Java.getAvailableJavaCups()[0];
        this.user = Accounts.readAccounts().getDefaultUser();
        this.path = MAIN_PATH.toString();
    }

    public long getMinRam() {
        if (this.min_ram == 0)
            return App.CONFIG.min_ram;
        return this.min_ram;
    }

    public long getMaxRam() {
        if (this.min_ram == 0)
            return App.CONFIG.max_ram;
        return this.max_ram;
    }

    public Java getJava() {
        if (this.java == null)
            return App.CONFIG.java;
        return this.java;
    }

    public User getUser() {
        if (this.user == null)
            return App.CONFIG.user;
        return this.user;
    }

    private void writeConfig() throws IOException {
        String json = App.GSON.toJson(this);

        Files.writeString(Paths.get(path), json, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    public void setMinRam(long min_ram) throws IOException {
        this.min_ram = min_ram;
        this.writeConfig();
    }

    public void setMaxRam(long max_ram) throws IOException {
        this.max_ram = max_ram;
        this.writeConfig();
    }

    public void setJava(Java java) throws IOException {
        this.java = java;
        this.writeConfig();
    }

    public void setUser(String username) throws IOException, NoSuchAccountException {
        this.user = Accounts.readAccounts().getUser(username);
        this.writeConfig();
    }

    public void setUsername(String username) throws IOException, NoSuchAccountException {
        this.user.setUsername(username);
        this.writeConfig();
    }

    public static Config readMainConfig() throws IOException {
        try {
            System.out.println("Config & App.DIR Path: " + MAIN_PATH);

            return Config.getConfig(MAIN_PATH);
        } catch (NoSuchFileException _e) {
            Config config = new Config();

            config.writeConfig();

            return config;
        }
    }

    public void launch(Instance instance) throws IOException {
        Client client = instance.readClient();
        Path[] paths = client.getLibrariesList();

        StringBuilder classpath = new StringBuilder();

        for (Path path : paths) {
            classpath.append(path);

            if (App.OS == Os.Windows)
                classpath.append(';');
            else
                classpath.append(':');
        }

        classpath.append(instance.Dir().resolve("client.jar"));

        String mainClass = client.mainClass;

        // TODO: Use client.arguments instead
        // TODO: Account arguments...
        ProcessBuilder javaProcess = new ProcessBuilder(
                this.getJava().path,
                "-Djava.library.path=" + instance.Dir().resolve(".natives"),
                "-Xms" + this.getMinRam() + "M",
                "-Xmx" + this.getMaxRam() + "M",
                "-cp", classpath.toString(),
                mainClass,
                "--username", this.getUser().getUsername(),
                "--gameDir", instance.Dir().toString(),
                "--assetsDir", App.ASSETS_DIR.toString(),
                "--assetIndex", client.assets,
                "--version", client.id,
                "--accessToken", "0");

        javaProcess.redirectErrorStream(true);
        javaProcess.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        System.out.println("Running:");
        System.out.println(javaProcess.command().toString());

        try {
            javaProcess.start().waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
