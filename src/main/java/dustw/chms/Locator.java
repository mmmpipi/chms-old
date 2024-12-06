package dustw.chms;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.mojang.logging.LogUtils;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.loading.ModDirTransformerDiscoverer;
import net.minecraftforge.fml.loading.StringUtils;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * @author DustW
 */
public class Locator extends AbstractJarFileModLocator {
    static final Logger LOGGER = LogUtils.getLogger();
    private final Path modFolder = FMLPaths.MODSDIR.get();

    private final Path configPath = FMLPaths.CONFIGDIR.get().resolve("CHMS.toml");

    private List<String> nameList;


    private void getConfig(){
        try {
            File file = configPath.toFile();
            if (!file.exists()){
                FileConfig fileConfig = FileConfig.of(configPath);
                fileConfig.load();
                fileConfig.set("black_list",List.of("disable",".connector"));
                fileConfig.save();
                fileConfig.close();
            }
            FileConfig config = FileConfig.of(configPath);
            config.load();
            this.nameList = config.get("black_list");
        } catch (Exception e) {
            LOGGER.error("cant load CMFS config:",e);
        }
        if (nameList == null){
            nameList = List.of();
        }
    }

    @Override
    public String name() {
        return "CHMS Mod Locator";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        // nothing to do
    }

    Stream<Path> getAllChild() {
        ArrayList<Path> result = new ArrayList<>();
        this.scanFolders().forEach(folder -> {
            try {
                result.addAll(Files.list(folder.toPath()).toList());
            } catch (IOException var3) {
                var3.printStackTrace();
            }
        });

        return result.stream();
    }

    ArrayList<File> scanFolders() {
        ArrayList<File> result = new ArrayList<>();
        this.scanFolder(this.modFolder, result);
        return result;
    }

    Path scanFolder(Path folder, ArrayList<File> fileList) {
        try (var files = Files.list(folder)) {
            files.map(Path::toFile)
                    .filter(f -> f.isDirectory() && !nameList.contains(f.getName()))
                    .map(file -> this.scanFolder(file.toPath(), fileList))
                    .forEach(file -> fileList.add(file.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return folder;
    }

    @Override
    public Stream<Path> scanCandidates() {
        LOGGER.debug("[CHMS] Scanning mods dir {} for mods", this.modFolder);
        getConfig();
        List<Path> excluded = ModDirTransformerDiscoverer.allExcluded();
        var a = LamdbaExceptionUtils.uncheck(this::getAllChild);
        return LamdbaExceptionUtils.uncheck(this::getAllChild)
                .filter(p -> !excluded.contains(p))
                .sorted(Comparator.comparing(path -> StringUtils.toLowerCase(path.getFileName().toString())))
                .filter(p -> StringUtils.toLowerCase(p.getFileName().toString()).endsWith(".jar"));
    }
}
