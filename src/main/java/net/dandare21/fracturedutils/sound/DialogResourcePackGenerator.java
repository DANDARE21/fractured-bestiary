package net.dandare21.fracturedutils.sound;

import net.dandare21.fracturedutils.FracturedUtils;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DialogResourcePackGenerator {

    private static final String PACK_FOLDER_NAME = "FracturedUtils_Dialog_Sounds";

    public static void generateIfMissing() {
        try {
            File resourcepacksDir = FMLPaths.GAMEDIR.get().resolve("resourcepacks").toFile();
            if (!resourcepacksDir.exists()) {
                resourcepacksDir.mkdirs();
            }

            File packDir = new File(resourcepacksDir, PACK_FOLDER_NAME);
            if (packDir.exists()) {
                // Resource pack directory already exists, do not overwrite user custom sound files
                return;
            }

            FracturedUtils.LOGGER.info("Generating external dialog sounds resource pack at {}", packDir.getAbsolutePath());

            File dialogSoundsDir = new File(packDir, "assets/fracturedutils/sounds/dialog");
            if (!dialogSoundsDir.mkdirs()) {
                FracturedUtils.LOGGER.error("Failed to create directory structure for resource pack: {}", dialogSoundsDir.getAbsolutePath());
                return;
            }

            // 1. Create pack.mcmeta
            File mcmetaFile = new File(packDir, "pack.mcmeta");
            String mcmetaContent = """
                    {
                      "pack": {
                        "pack_format": 15,
                        "description": "Fractured Utils Custom Dialog Sounds & Voice Blips Resource Pack"
                      }
                    }
                    """;
            writeFile(mcmetaFile, mcmetaContent);

            // 2. Create assets/fracturedutils/sounds.json
            File soundsJsonFile = new File(packDir, "assets/fracturedutils/sounds.json");
            String soundsJsonContent = """
                    {
                      "dialog.blip_default": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_default" ]
                      },
                      "dialog.blip_low": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_low" ]
                      },
                      "dialog.blip_high": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_high" ]
                      },
                      "dialog.blip_sans": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_sans" ]
                      },
                      "dialog.blip_papyrus": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_papyrus" ]
                      },
                      "dialog.blip_robot": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_robot" ]
                      },
                      "dialog.blip_typing": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_typing" ]
                      },
                      "dialog.blip_monster": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/blip_monster" ]
                      },
                      "dialog.custom_voice1": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/custom_voice1" ]
                      },
                      "dialog.custom_voice2": {
                        "category": "player",
                        "sounds": [ "fracturedutils:dialog/custom_voice2" ]
                      }
                    }
                    """;
            writeFile(soundsJsonFile, soundsJsonContent);

            // 3. Create README.txt guide
            File readmeFile = new File(dialogSoundsDir, "README.txt");
            String readmeContent = """
                    ==================================================
                    Fractured Utils - Custom Dialog Sounds Resource Pack
                    ==================================================
                    
                    Place your custom dialog sound audio files (.ogg format) in this folder!

                    Standard voice blip file names expected:
                    - blip_default.ogg
                    - blip_low.ogg
                    - blip_high.ogg
                    - blip_sans.ogg
                    - blip_papyrus.ogg
                    - blip_robot.ogg
                    - blip_typing.ogg
                    - blip_monster.ogg
                    - custom_voice1.ogg
                    - custom_voice2.ogg

                    To add additional custom dialog sounds:
                    1. Drop your .ogg sound file into this folder.
                    2. Add a new sound entry to assets/fracturedutils/sounds.json in this resource pack.
                    3. Enable 'Fractured Utils Custom Dialog Sounds' in Minecraft's Resource Packs menu!
                    ==================================================
                    """;
            writeFile(readmeFile, readmeContent);

            FracturedUtils.LOGGER.info("Successfully generated external dialog sounds resource pack in {}", packDir.getAbsolutePath());

        } catch (Exception e) {
            FracturedUtils.LOGGER.error("Failed to generate dialog sounds resource pack: {}", e.getMessage(), e);
        }
    }

    private static void writeFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
}
