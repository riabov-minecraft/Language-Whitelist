package com.riabov.langwhitelist;

import net.minecraftforge.fml.loading.FMLPaths;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LangWhitelistConfig {
    private static final String FILE_NAME = "langwhitelist-client.toml";
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);

    private static final String KEY = "allowed_languages";
    private static final Pattern KEY_LINE = Pattern.compile("^\\s*" + KEY + "\\s*=\\s*(.+)\\s*$");
    private static final Pattern LANG_CODE = Pattern.compile("^[a-z]{2,3}_[a-z]{2,3}$");

    private static volatile boolean loadedOnce;
    private static volatile Set<String> cachedAllowedOrNull;

    private LangWhitelistConfig() {
    }

    public static Set<String> getAllowedOrNull() {
        if (!loadedOnce) {
            load();
        }

        return cachedAllowedOrNull;
    }

    public static synchronized void load() {
        if (loadedOnce) {
            return;
        }

        loadedOnce = true;

        try {
            ensureConfigExists();

            Set<String> parsed = parseAllowedLanguages(CONFIG_PATH);
            cachedAllowedOrNull = parsed;

            if (parsed == null) {
                LangWhitelistMod.LOGGER.info(
                        "[Language Whitelist] Config loaded: filter disabled (show all languages)."
                );
            } else {
                LangWhitelistMod.LOGGER.info(
                        "[Language Whitelist] Config loaded: allowed_languages={}",
                        parsed
                );
            }
        } catch (Exception exception) {
            LangWhitelistMod.LOGGER.warn(
                    "[Language Whitelist] Failed to load config, restoring defaults. Reason: {}",
                    exception.toString()
            );

            try {
                backupBrokenConfig();
                writeDefaultConfig();
            } catch (Exception backupException) {
                LangWhitelistMod.LOGGER.warn(
                        "[Language Whitelist] Also failed to backup/restore defaults: {}",
                        backupException.toString()
                );
            }

            cachedAllowedOrNull = null;
        }
    }

    private static void ensureConfigExists() throws IOException {
        Path parent = CONFIG_PATH.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        if (!Files.exists(CONFIG_PATH)) {
            writeDefaultConfig();
        }
    }

    private static void writeDefaultConfig() throws IOException {
        List<String> lines = Arrays.asList(
                "# List of language codes that should be visible in Language menu.",
                "# Examples: en_us, ru_ru",
                "# Special values: \"*\" or \"all\" = show all languages.",
                "# Empty list = show all languages.",
                KEY + " = [\"en_us\", \"ru_ru\"]",
                ""
        );

        try (BufferedWriter writer = Files.newBufferedWriter(
                CONFIG_PATH,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        )) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static void backupBrokenConfig() throws IOException {
        if (!Files.exists(CONFIG_PATH)) {
            return;
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = CONFIG_PATH.resolveSibling(FILE_NAME + ".broken-" + timestamp);

        Files.move(CONFIG_PATH, backup, StandardCopyOption.REPLACE_EXISTING);
        LangWhitelistMod.LOGGER.warn(
                "[Language Whitelist] Broken config backed up as: {}",
                backup.getFileName().toString()
        );
    }

    private static Set<String> parseAllowedLanguages(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

        String rightHandSide = null;
        for (String rawLine : lines) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher matcher = KEY_LINE.matcher(line);
            if (matcher.matches()) {
                rightHandSide = matcher.group(1).trim();
                break;
            }
        }

        if (rightHandSide == null) {
            return null;
        }

        int leftBracket = rightHandSide.indexOf('[');
        int rightBracket = rightHandSide.lastIndexOf(']');
        if (leftBracket < 0 || rightBracket < 0 || rightBracket <= leftBracket) {
            throw new IllegalArgumentException(
                    "Invalid " + KEY + " format. Expected [\"en_us\", \"ru_ru\"]."
            );
        }

        String contents = rightHandSide.substring(leftBracket + 1, rightBracket).trim();
        if (contents.isEmpty()) {
            return null;
        }

        String[] parts = contents.split(",");
        Set<String> allowedLanguages = new HashSet<>();

        for (String part : parts) {
            String languageCode = part.trim();
            if (languageCode.isEmpty()) {
                continue;
            }

            if ((languageCode.startsWith("\"") && languageCode.endsWith("\""))
                    || (languageCode.startsWith("'") && languageCode.endsWith("'"))) {
                languageCode = languageCode.substring(1, languageCode.length() - 1).trim();
            }

            if (languageCode.isEmpty()) {
                continue;
            }

            String normalizedCode = languageCode.toLowerCase(Locale.ROOT);
            if (normalizedCode.equals("*") || normalizedCode.equals("all")) {
                return null;
            }

            if (LANG_CODE.matcher(normalizedCode).matches()) {
                allowedLanguages.add(normalizedCode);
            } else {
                LangWhitelistMod.LOGGER.warn(
                        "[Language Whitelist] Ignoring invalid language code in config: '{}'",
                        normalizedCode
                );
            }
        }

        if (allowedLanguages.isEmpty()) {
            return null;
        }

        return Collections.unmodifiableSet(allowedLanguages);
    }

    private static String stripComment(String line) {
        int commentIndex = line.indexOf('#');
        return commentIndex >= 0 ? line.substring(0, commentIndex) : line;
    }
}
