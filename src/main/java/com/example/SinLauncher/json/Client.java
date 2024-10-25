// Client.java

package com.example.SinLauncher.json;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.example.SinLauncher.App;
import com.example.SinLauncher.SinLauncherEntites.Arch;
import com.example.SinLauncher.SinLauncherEntites.Os;
import com.example.SinLauncher.json.AssetIndex.AssetObject;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import kong.unirest.core.Unirest;
import lombok.ToString;

@ToString
public class Client {

    @ToString
    public static class Features {
        @SerializedName("is_demo_user")
        public Boolean isDemoUser = false;

        @SerializedName("has_custom_resolution")
        public Boolean hasCustomResolution = false;

        @SerializedName("has_quick_plays_support")
        public Boolean hasQuickPlaysSupport = false;

        @SerializedName("is_quick_play_singleplayer")
        public Boolean isQuickPlaySingleplayer = false;

        @SerializedName("is_quick_play_multiplayer")
        public Boolean isQuickPlayMultiplayer = false;

        @SerializedName("is_quick_play_realms")
        public Boolean isQuickPlayRealms = false;
    }

    @ToString
    public static class OSRules {
        public Os name;
        public Arch arch;
        public String version;
    }

    @ToString
    public static class Rule {
        public String action;
        public OSRules os;
        public Features features;

        // Checks if OS matches OSRules & also respects action
        public boolean osMatches() {
            boolean match = true;

            if (os != null) {
                if (os.name != null)
                    match = match && os.name == App.OS;

                if (os.arch != null)
                    match = match && os.arch == App.ARCH;

                if (action.equals("allow"))
                    return match;

                if (action.equals("disallow"))
                    return !match;
            }

            return match;
        }

        // Mass check {@code Rule.osMatches} on each rule in {@code rules}
        public static boolean osMatches(Rule[] rules) {
            for (Rule rule : rules) {
                if (!rule.osMatches())
                    return false;
            }

            return true;
        }
    }

    @ToString
    public static class Argument {
        public String value;
        public String[] values;
        public Rule[] rules;
    }

    /*
     * Each *Argument* may be a plain string or a struct which looks like this
     * 
     * {
     * Rule[] rules;
     * String | String[] value;
     * }
     * 
     * We want to deserialize this into the *Argument* class
     */
    public static class ArgumentDeserializer implements JsonDeserializer<Argument> {

        @Override
        public Argument deserialize(JsonElement element, Type type, JsonDeserializationContext context)
                throws JsonParseException {
            var argument = new Argument();

            if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString())
                argument.value = element.getAsString();

            else if (element.isJsonObject()) {
                JsonObject jsonObject = element.getAsJsonObject();

                if (jsonObject.has("value")) {
                    JsonElement valueObj = jsonObject.get("value");

                    if (valueObj.isJsonPrimitive())
                        argument.value = valueObj.getAsString();

                    else if (valueObj.isJsonArray())
                        argument.values = context.deserialize(valueObj.getAsJsonArray(), String[].class);

                }

                if (jsonObject.has("rules"))
                    argument.rules = context.deserialize(jsonObject.get("rules"), Rule[].class);

            }

            return argument;
        }
    }

    @ToString
    public static class Arguments {
        public Argument[] game;
        public Argument[] jvm;
    }

    @ToString
    public static class Download {
        public String id;
        public String path;
        public String sha1;
        public long size;
        public Long totalSize;
        public String url;

        // Fetches a download returns an IOException if it fails to download
        public byte[] fetch() throws IOException {
            var response = Unirest.get(url).asBytes();

            if (response.getStatus() != 200)
                throw new IOException("failed to download " + this.url);

            return response.getBody();
        }
    }

    @ToString
    public static class ClientDownloads {
        public Download client;
        public Download client_mappings;
        public Download server;
        public Download server_mappings;
    }

    @ToString
    public static class JavaVersion {
        public String component;
        public int majorVersion;
    }

    @ToString
    public static class LibraryDownloads {
        public Download artifact;
        public Map<String, Download> classifiers;

        public byte[] fetchArtifact() throws IOException {
            return artifact.fetch();
        }

        public Path artifactPath() {
            return Paths.get(App.LIBRARIES_DIR.toString(), artifact.path);
        }

        public byte[] fetchNative(String nativeIndex) throws IOException {
            return classifiers.get(nativeIndex).fetch();
        }

        // This may return NULL if there is no such classifier
        public Path nativePath(String nativeIndex) {
            Download download = classifiers.get(nativeIndex);

            if (download == null)
                return null;

            return Paths.get(App.NATIVES_DIR.toString(), download.path);
        }
    }

    public static class LibraryExtractRules {
        public String[] exclude;
        public String[] include;
    }

    public static class Library {
        public LibraryDownloads downloads;
        public String name;
        public Rule[] rules;
        public Map<Os, String> natives;
        public LibraryExtractRules extract;

        public void downloadArtifact() throws IOException {
            if (downloads.artifact == null)
                return;

            Path artifactPath = downloads.artifactPath();

            if (!Files.exists(artifactPath)) {
                var artifact = downloads.fetchArtifact();

                Files.createDirectories(artifactPath.getParent());
                Files.write(artifactPath, artifact);
            }
        }

        /**
         * Downloads native library to archive SINLAUNCHER/natives
         * manual extraction required to INSTANCE/.natives
         * (SINLAUNCHER/instances/INSTANCE/.natives)
         * won't throw an exception if there is no natives
         */
        public void downloadNative() throws IOException {
            if (natives == null)
                return;

            String nativeIndex = natives.get(App.OS);

            if (nativeIndex != null) {
                Path nativePath = downloads.nativePath(nativeIndex);

                if (nativePath != null && !Files.exists(nativePath)) {
                    var fetched = downloads.fetchNative(nativeIndex);
                    Files.createDirectories(nativePath.getParent());
                    Files.write(nativePath, fetched);
                }
            }
        }

        // Extracts native libs to {@code instanceDir}/.natives excluding {@code
        // this.exclude}

        public void extractNative(Path instanceDir) throws IOException {
            if (natives == null)
                return;

            String nativeIndex = natives.get(App.OS);

            if (nativeIndex != null) {
                Path nativeZipPath = downloads.nativePath(nativeIndex);

                if (nativeZipPath == null)
                    return;

                Path nativeDestDir = Paths.get(instanceDir.toString(), ".natives");

                if (!Files.exists(nativeDestDir))
                    Files.createDirectories(nativeDestDir);

                // TODO: Maybe actually move unzipping into helper methods?

                // The actual unzipping
                ZipInputStream nativeZip = new ZipInputStream(new FileInputStream(nativeZipPath.toString()));
                ZipEntry zipEntry = nativeZip.getNextEntry();

                var buffer = new byte[1024];

                ziploop: while (zipEntry != null) {
                    String name = zipEntry.getName();
                    Path nameAsPath = Paths.get(name);

                    if (this.extract != null) {
                        // Excluding
                        for (String exlude : this.extract.exclude) {
                            if (nameAsPath.startsWith(exlude)) {
                                zipEntry = nativeZip.getNextEntry();
                                continue ziploop;
                            }
                        }
                    }
                    File newFile = new File(nativeDestDir.toString(), name);

                    if (!newFile.exists()) {

                        if (zipEntry.isDirectory()) {

                            if (!newFile.mkdirs()) {
                                nativeZip.closeEntry();
                                nativeZip.close();
                                throw new IOException("Failed to create directory " + newFile);
                            }

                        }

                        else {
                            File parent = newFile.getParentFile();

                            if (!parent.exists() && !parent.mkdirs()) {
                                nativeZip.closeEntry();
                                nativeZip.close();
                                throw new IOException("Failed to create directory " + parent);
                            }

                            // Write file content
                            FileOutputStream fos = new FileOutputStream(newFile);
                            int len;

                            while ((len = nativeZip.read(buffer)) > 0)
                                fos.write(buffer, 0, len);

                            fos.close();

                        }

                    }

                    zipEntry = nativeZip.getNextEntry();
                }

                nativeZip.closeEntry();
                nativeZip.close();
            }
        }

    }

    public static class LoggingClient {
        public String argument;
        public Download file;
        public String type;
    }

    public static class LoggingInfo {
        public LoggingClient client;
    }

    // The actual client.json
    public Arguments arguments;
    public Download assetIndex;
    public String assets;

    public short complianceLevel;

    public ClientDownloads downloads;
    public String id;
    public JavaVersion javaVersion;

    public Library[] libraries;
    public LoggingInfo logging;

    public String mainClass;

    // Removed in 1.13, replaced with arguments above
    public String minecraftArguments;

    // TODO: Use an enum
    public String type;

    // Downloads the client assets
    public void downloadAssets() throws IOException {

        // Downloads the assets
        Path indexesDir = Paths.get(App.ASSETS_DIR.toString(), "indexes");

        if (!Files.exists(indexesDir))
            Files.createDirectories(indexesDir);

        Path indexPath = Paths.get(indexesDir.toString(), this.assets + ".json");

        if (!Files.exists(indexPath)) {
            var indexFile = this.assetIndex.fetch();

            Files.write(indexPath, indexFile);
        }

        var json = Files.readString(indexPath);
        AssetIndex asset = App.GSON.fromJson(json, AssetIndex.class);

        for (AssetObject object : asset.objects.values())
            object.fetch();

    }

    // Downloads the client libraries including native libraries
    public void downloadLibraries(Path instanceDir) throws IOException {
        for (Library library : libraries) {
            if (library.rules != null) {
                if (!Rule.osMatches(library.rules))
                    continue;
            }

            library.downloadArtifact();
            library.downloadNative();
            library.extractNative(instanceDir);
        }
    }

    // Downloads the client.jar
    public void downloadClientDownloads(Path instanceDir) throws IOException {
        Path clientJarPath = Paths.get(instanceDir.toString(), "client.jar");

        if (!Files.exists(clientJarPath)) {
            var fetched = downloads.client.fetch();
            Files.write(clientJarPath, fetched);
        }
    }

    /**
     * Downloads libraries, natives, assets, client.jar, etc provided by
     * {@code} this
     * will not re-download if any already exists
     */

    public void download(Path instanceDir) throws IOException {
        downloadAssets();
        downloadLibraries(instanceDir);
        downloadClientDownloads(instanceDir);
    }

    // Gives a list of the required libraries paths in {@code App.LIBRARIES_DIR}
    public Path[] getLibrariesList() {
        List<Path> pathList = new ArrayList<Path>();

        for (Library library : libraries) {
            if (library.downloads.artifact != null) {
                if (library.rules != null)
                    if (!Rule.osMatches(library.rules))
                        continue;

                pathList.add(library.downloads.artifactPath());
            }
        }

        return pathList.toArray(new Path[0]);
    }
}
