package com.duke.screenmatch.settings;

import com.duke.screenmatch.utils.Utils;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 读取配置文件  screenMatch.properties , screenMatch_example_dimens.xml
 */
public class Settings {

    /**
     * 文件名不要随便改，请注意IO流读写处使用的输入流文件名称
     */
    public static String PROPERTIES_FILE_NAME = "screenMatch.properties";
    public static String PROPERTIES_FILE_OLD_NAME = "screenMatch_old.properties";
    public static String DIMENS_FILE_NAME = "screenMatch_example_dimens.xml";


    public static String KEY_BASE_DP = "base_dp";
    public static String KEY_MATCH_MODULE = "match_module";
    public static String KEY_MATCH_DP = "match_dp";
    public static String KEY_IGNORE_DP = "ignore_dp";
    public static String KEY_IGNORE_MODULE_NAME = "ignore_module_name";
    public static String KEY_NOT_SHOW_DIALOG = "not_show_dialog";
    public static String KEY_IS_MATCH_FONT_SP = "is_match_font_sp";
    public static String KEY_NOT_CREATE_DEFAULT_DIMENS = "not_create_default_dimens";
    public static String KEY_CREATE_VALUES_SW_FOLDER = "create_values_sw_folder";
    public static String KEY_KEEP_SOURCE_COMMENT = "keep_source_comment";
    public static String KEY_ADD_DEFAULT_MATCH_DP_ARRAYS = "add_default_match_dp_array";
    public static String KEY_AUTO_RELOAD_PROJECT_AFTER_WORK_FINISHED = "auto_reload_project_after_work_finished";

    // key to save the plugin version
    private static final String KEY_PLUGIN_VERSION = "screen_match_version";

    private static final int PLUGIN_VERSION_31 = 31;
    private static final int PLUGIN_VERSION_32 = 32;
    private static final int CURRENT_PLUGIN_VERSION = PLUGIN_VERSION_32;

    private static Map<String, String> sSettingMap;

    public static void invalidateSettingCache(String basePath) {
        HashMap<String, String> map = readSettings(basePath);
        if (map != null) {
            sSettingMap = map;
        }
    }

    public static String get(String key) {
        Map<String, String> hashMap = sSettingMap;
        if (hashMap == null || hashMap.isEmpty()) {
            return null;
        }
        return hashMap.get(key);
    }

    public static HashMap<String, String> readSettings(String basePath) {
        if (Utils.isEmpty(basePath)) {
            return null;
        }

        String project_file_path = Utils.ensurePathEndSeparator(basePath) + PROPERTIES_FILE_NAME;
        VirtualFile virtualFile = Utils.getVirtualFile(project_file_path);
        if (virtualFile == null || !virtualFile.isValid()) {
            writeSettings(basePath);
            writeDefaultDimens(basePath);
            return null;
        }

        HashMap<String, String> map = new HashMap<>();
        // virtualFile.getInputStream() non-null
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(virtualFile.getInputStream()))) {
            String temp;
            while ((temp = reader.readLine()) != null) {
                if (Utils.isEmpty(temp) || temp.startsWith("#") || !temp.contains("=")) {
                    continue;
                }
                String[] values = temp.split("=");
                if (values.length != 2) {
                    continue;
                }
                String key = values[0].trim();
                String value = values[1].trim();
                if (!KEY_BASE_DP.equals(key)
                        && !KEY_MATCH_DP.equals(key)
                        && !KEY_MATCH_MODULE.equals(key)
                        && !KEY_IGNORE_DP.equals(key)
                        && !KEY_IGNORE_MODULE_NAME.equals(key)
                        && !KEY_NOT_SHOW_DIALOG.equals(key)
                        && !KEY_IS_MATCH_FONT_SP.equals(key)
                        && !KEY_CREATE_VALUES_SW_FOLDER.equals(key)
                        && !KEY_NOT_CREATE_DEFAULT_DIMENS.equals(key)
                        && !KEY_KEEP_SOURCE_COMMENT.equals(key)
                        && !KEY_AUTO_RELOAD_PROJECT_AFTER_WORK_FINISHED.equals(key)
                        && !KEY_ADD_DEFAULT_MATCH_DP_ARRAYS.equals(key)) {
                    continue;
                }
                if (Utils.isEmpty(value)) {
                    continue;
                }
                map.put(key, value);
            }
            String not_create_dimens = map.get(KEY_NOT_CREATE_DEFAULT_DIMENS);
            boolean notCreateDimens = false;
            try {
                notCreateDimens = Boolean.parseBoolean(not_create_dimens.trim());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!notCreateDimens) {
                writeDefaultDimens(basePath);
            }
            //addGitignore(basePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            virtualFile = Utils.getVirtualFile(basePath);
            if (virtualFile != null) {
                virtualFile.refresh(true, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    public static void addGitignore(String basePath) {
        if (Utils.isEmpty(basePath)) {
            return;
        }
        String project_file_path = Utils.ensurePathEndSeparator(basePath) + ".gitignore";
        VirtualFile virtualFile = Utils.getVirtualFile(project_file_path);
        if (virtualFile == null || !virtualFile.isValid()) {
            return;
        }

        try(BufferedReader reader = new BufferedReader(new InputStreamReader(virtualFile.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(project_file_path, true))) {

            String temp;
            boolean hasPropertyFile = false;
            boolean hasDimensFile = false;
            while ((temp = reader.readLine()) != null) {
                if (temp.contains(PROPERTIES_FILE_NAME)) {
                    hasPropertyFile = true;
                }
                if (temp.contains(DIMENS_FILE_NAME)) {
                    hasDimensFile = true;
                }
            }
            if (!hasPropertyFile || !hasDimensFile) {
                writer.newLine();
            }
            String text;
            if (!hasPropertyFile) {
                text = PROPERTIES_FILE_NAME;
                writer.write(text, 0, text.length());
                writer.newLine();
                writer.flush();
            }
            if (!hasDimensFile) {
                text = DIMENS_FILE_NAME;
                writer.write(text, 0, text.length());
                writer.newLine();
                writer.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void migrationIfNeed(Project project) {
        // check if needs do a version update migration
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        int version = properties.getInt(KEY_PLUGIN_VERSION, PLUGIN_VERSION_31);
        if (version != PLUGIN_VERSION_32) {
            VirtualFile virtualFile = Utils.getVirtualFile(project.getBasePath());
            if (virtualFile != null && virtualFile.isValid()) {
                try {
                    VirtualFile propertiesFile = virtualFile.findChild(PROPERTIES_FILE_NAME);
                    if (propertiesFile != null && propertiesFile.isValid()) {
                        // rename old format settings file to keep user's configuration
                        WriteAction.run(() -> {
                            VirtualFile oldFile = virtualFile.findChild(PROPERTIES_FILE_OLD_NAME);
                            if (oldFile != null && oldFile.exists()) {
                                oldFile.delete(project);
                            }
                            propertiesFile.rename(project, PROPERTIES_FILE_OLD_NAME);
                        });
                    }
                    // write new format settings file
                    writeSettings(Utils.getBasePath(project));
                    properties.setValue(KEY_PLUGIN_VERSION, CURRENT_PLUGIN_VERSION, PLUGIN_VERSION_31);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static boolean writeSettings(String basePath) {
        return write(basePath, PROPERTIES_FILE_NAME);
    }

    public static boolean writeDefaultDimens(String basePath) {
        return write(basePath, DIMENS_FILE_NAME);
    }

    public static boolean write(String basePath, String fileName) {
        if (Utils.isEmpty(basePath)) {
            return false;
        }

        String project_file_path = Utils.ensurePathEndSeparator(basePath) + fileName;
        File file = new File(project_file_path);
        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Settings.class.getResourceAsStream("/" + fileName)));
             BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String temp;
            while ((temp = reader.readLine()) != null) {
                writer.write(temp, 0, temp.length());
                writer.newLine();
            }
            writer.flush();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void setDefaultModuleName(String moduleName) {
        if (Utils.isEmpty(moduleName)) {
            return;
        }
        PropertiesComponent.getInstance().setValue("moduleName", moduleName);
    }

    public static String getDefaultModuleName() {
        return PropertiesComponent.getInstance().getValue("moduleName");
    }
}
