package com.yayfolk.backend.util;

import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public final class TextRepairUtils {
    private static final Charset GBK = Charset.forName("GBK");
    private static final String COMMON_READABLE_CJK =
        "\u5728\u7EBF\u5BA2\u670D\u8BC4\u8BBA\u6536\u85CF\u901A\u77E5\u6D88\u606F\u4F1A\u8BDD\u52A0\u8F7D" +
        "\u53D1\u9001\u521B\u5EFA\u5931\u8D25\u6682\u65E0\u8BF7\u7A0D\u540E\u518D\u8BD5\u5DF2\u6807\u8BB0\u8BFB" +
        "\u5220\u9664\u6E05\u7A7A\u4F60\u597D\u5417\u56DE\u590D\u540C\u6B65\u5E73\u53F0\u6C9F\u901A\u8F93\u5165" +
        "\u8BB0\u5F55\u9009\u62E9\u5E38\u51E0\u5206\u949F";
    private static final String COMMON_PUNCTUATION =
        " ,.!?;:'\"()[]{}<>-_/\\@#$%^&*+=~`|" +
        "\uFF0C\u3002\uFF01\uFF1F\u3001\uFF1B\uFF1A\uFF08\uFF09\u300A\u300B" +
        "\u201C\u201D\u2018\u2019\u3010\u3011\u00B7\u2026";

    private TextRepairUtils() {
    }

    public static String repairIfNeeded(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        String gbkCandidate = transcode(text, GBK, StandardCharsets.UTF_8);
        if (isBetter(text, gbkCandidate, GBK)) {
            return gbkCandidate;
        }

        String latinCandidate = transcode(text, StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8);
        if (isBetter(text, latinCandidate, StandardCharsets.ISO_8859_1)) {
            return latinCandidate;
        }

        return text;
    }

    private static String transcode(String text, Charset source, Charset target) {
        try {
            return new String(text.getBytes(source), target);
        } catch (Exception ignored) {
            return text;
        }
    }

    private static boolean isBetter(String original, String candidate, Charset sourceCharset) {
        if (!StringUtils.hasText(candidate) || original.equals(candidate)) {
            return false;
        }
        if (!roundTripMatches(original, candidate, sourceCharset)) {
            return false;
        }

        int originalScore = readabilityScore(original);
        int candidateScore = readabilityScore(candidate);
        if (candidateScore <= originalScore + 6) {
            return false;
        }

        return hasBrokenSignals(original) || candidateScore > originalScore + 12;
    }

    private static boolean roundTripMatches(String original, String candidate, Charset sourceCharset) {
        try {
            String roundTrip = new String(candidate.getBytes(StandardCharsets.UTF_8), sourceCharset);
            return original.equals(roundTrip);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int readabilityScore(String text) {
        int score = 0;

        for (char c : text.toCharArray()) {
            if (COMMON_READABLE_CJK.indexOf(c) >= 0) {
                score += 4;
                continue;
            }
            if (isReadableEastAsianChar(c) || Character.isLetterOrDigit(c)) {
                score += 2;
                continue;
            }
            if (Character.isWhitespace(c) || COMMON_PUNCTUATION.indexOf(c) >= 0) {
                score += 1;
                continue;
            }
            if (isBrokenChar(c)) {
                score -= 8;
                continue;
            }
            score -= 2;
        }

        if (hasBrokenSignals(text)) {
            score -= 12;
        }

        return score;
    }

    private static boolean hasBrokenSignals(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        int cjkCount = 0;
        int hiraganaCount = 0;
        int katakanaCount = 0;

        for (char c : text.toCharArray()) {
            if (isBrokenChar(c)) {
                return true;
            }
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) {
                cjkCount++;
            } else if (block == Character.UnicodeBlock.HIRAGANA) {
                hiraganaCount++;
            } else if (block == Character.UnicodeBlock.KATAKANA) {
                katakanaCount++;
            }
        }

        return cjkCount > 0 && katakanaCount > 0 && hiraganaCount == 0;
    }

    private static boolean isBrokenChar(char c) {
        return c == '\uFFFD' || c == '\u20AC' || (c >= '\uE000' && c <= '\uF8FF');
    }

    private static boolean isReadableEastAsianChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
            || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
            || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
            || block == Character.UnicodeBlock.HIRAGANA
            || block == Character.UnicodeBlock.KATAKANA
            || block == Character.UnicodeBlock.HANGUL_SYLLABLES
            || block == Character.UnicodeBlock.HANGUL_JAMO
            || block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO;
    }
}