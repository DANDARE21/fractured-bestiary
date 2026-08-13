package net.dandare21.fracturedutils.dialog;

import net.minecraft.network.chat.Component;

public class DialogFormatUtil {

    /**
     * Translates '&' color and formatting codes into '§' section symbols.
     */
    public static String translateCodes(String input) {
        if (input == null) return "";
        return input.replaceAll("(?i)&([0-9a-fk-or])", "§$1");
    }

    /**
     * Formats a raw string into a Minecraft Component with styling applied.
     */
    public static Component formatText(String input) {
        return Component.literal(translateCodes(input));
    }

    /**
     * Combines speaker and text into a styled line component.
     */
    public static Component formatLine(String speaker, String text) {
        String translatedSpeaker = translateCodes(speaker);
        String translatedText = translateCodes(text);

        if (translatedSpeaker != null && !translatedSpeaker.trim().isEmpty()) {
            return Component.literal(translatedSpeaker + " §r" + translatedText);
        } else {
            return Component.literal(translatedText);
        }
    }

    /**
     * Counts the visible characters in a string, ignoring '&' and '§' formatting codes.
     */
    public static int getVisibleCharCount(String input) {
        if (input == null || input.isEmpty()) return 0;
        int count = 0;
        int len = input.length();
        for (int i = 0; i < len; i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len && isFormatChar(input.charAt(i + 1))) {
                i++; // Skip format code character
            } else {
                count++;
            }
        }
        return count;
    }

    /**
     * Reveals up to visibleCharCount characters of formatted text without breaking formatting codes.
     */
    public static String getRevealedText(String input, int visibleCharCount) {
        if (input == null || input.isEmpty() || visibleCharCount <= 0) return "";
        StringBuilder sb = new StringBuilder();
        int remainingChars = visibleCharCount;
        int len = input.length();

        for (int i = 0; i < len && remainingChars > 0; i++) {
            char c = input.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < len && isFormatChar(input.charAt(i + 1))) {
                sb.append(c).append(input.charAt(i + 1));
                i++;
            } else {
                sb.append(c);
                remainingChars--;
            }
        }

        return translateCodes(sb.toString());
    }

    private static boolean isFormatChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')
                || (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O') || c == 'r' || c == 'R';
    }
}
