// AssetIndex.java

package com.example.SinLauncher.json;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import com.example.SinLauncher.App;

import kong.unirest.core.Unirest;

public class AssetIndex {
    public static class AssetObject {
        String hash;

        public String id() {
            return new String(new char[] {
                    this.hash.charAt(0), this.hash.charAt(1)
            });
        }

        // Get the URL of the asset object
        public String url() {
            return "https://resources.download.minecraft.net/" + this.id() + "/" + hash;
        }

        // Gets the Path of the asset
        public Path path() {
            return Paths.get(
                    App.ASSETS_DIR.toString(),
                    "objects",
                    this.id(),
                    hash
            );
        }

        // Downloads an asset object and puts it in {@code this.path()}
        public void fetch() throws IOException {
            if (!Files.exists(this.path())) {
                var fetch = Unirest.get(this.url()).asBytes();

                if (fetch.getStatus() != 200)
                    throw new IOException("failed to download an asset object with hash " + this.hash);

                Files.createDirectories(this.path().getParent());
                Files.write(this.path(), fetch.getBody());
            }
        }
    }

    Map<String, AssetObject> objects;
}
