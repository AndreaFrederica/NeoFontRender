package neofontrender.core.font.linebreak;

/**
 * Lightweight CJK line-start/line-end prohibition rules.
 *
 * <p>This deliberately only decides whether an otherwise valid visible character
 * boundary is a useful wrapping opportunity. Width measurement and raw-to-visible
 * index mapping remain the responsibility of the active font renderer.</p>
 */
public final class CjkLineBreakRules {
    private CjkLineBreakRules() {
    }

    public static boolean canBreakBetween(int previous, int next) {
        if (previous < 0 || next < 0 || isNoBreakControl(previous) || isNoBreakControl(next)) {
            return false;
        }
        if (Character.isWhitespace(previous) || Character.isWhitespace(next)) {
            return false;
        }
        if (isCombiningMark(next) || isForbiddenAtLineStart(next) || isForbiddenAtLineEnd(previous)) {
            return false;
        }
        return isCjk(previous) || isCjk(next)
                || isCjkPunctuation(previous) || isCjkPunctuation(next);
    }

    public static boolean isForbiddenAtLineStart(int codePoint) {
        if (isCombiningMark(codePoint)) {
            return true;
        }
        switch (codePoint) {
            case ')':
            case ']':
            case '}':
            case ',':
            case '.':
            case ':':
            case ';':
            case '!':
            case '?':
            case '%':
            case 0x00BB: // »
            case 0x2019: // ’
            case 0x201D: // ”
            case 0x2025: // ‥
            case 0x2026: // …
            case 0x2030: // ‰
            case 0x2031: // ‱
            case 0x203C: // ‼
            case 0x2047: // ⁇
            case 0x2048: // ⁈
            case 0x2049: // ⁉
            case 0x3001: // 、
            case 0x3002: // 。
            case 0x3005: // 々
            case 0x3009: // 〉
            case 0x300B: // 》
            case 0x300D: // 」
            case 0x300F: // 』
            case 0x3011: // 】
            case 0x3015: // 〕
            case 0x3017: // 〗
            case 0x3019: // 〙
            case 0x301B: // 〛
            case 0x301E: // 〞
            case 0x301F: // 〟
            case 0x303B: // 〻
            case 0x30FB: // ・
            case 0x30FC: // ー
            case 0x30FD: // ヽ
            case 0x30FE: // ヾ
            case 0xFF01: // ！
            case 0xFF05: // ％
            case 0xFF09: // ）
            case 0xFF0C: // ，
            case 0xFF0E: // ．
            case 0xFF1A: // ：
            case 0xFF1B: // ；
            case 0xFF1F: // ？
            case 0xFF3D: // ］
            case 0xFF5D: // ｝
            case 0xFF60: // ｠
                return true;
            default:
                return isSmallKana(codePoint);
        }
    }

    public static boolean isForbiddenAtLineEnd(int codePoint) {
        switch (codePoint) {
            case '(':
            case '[':
            case '{':
            case 0x00AB: // «
            case 0x2018: // ‘
            case 0x201C: // “
            case 0x3008: // 〈
            case 0x300A: // 《
            case 0x300C: // 「
            case 0x300E: // 『
            case 0x3010: // 【
            case 0x3014: // 〔
            case 0x3016: // 〖
            case 0x3018: // 〘
            case 0x301A: // 〚
            case 0x301D: // 〝
            case 0xFF08: // （
            case 0xFF3B: // ［
            case 0xFF5B: // ｛
            case 0xFF5F: // ｟
                return true;
            default:
                return false;
        }
    }

    private static boolean isCjk(int codePoint) {
        Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
        return script == Character.UnicodeScript.HAN
                || script == Character.UnicodeScript.HIRAGANA
                || script == Character.UnicodeScript.KATAKANA
                || script == Character.UnicodeScript.HANGUL
                || script == Character.UnicodeScript.BOPOMOFO;
    }

    private static boolean isCjkPunctuation(int codePoint) {
        return codePoint >= 0x3000 && codePoint <= 0x303F
                || codePoint >= 0xFE10 && codePoint <= 0xFE1F
                || codePoint >= 0xFE30 && codePoint <= 0xFE4F
                || codePoint >= 0xFF01 && codePoint <= 0xFF65;
    }

    private static boolean isCombiningMark(int codePoint) {
        int type = Character.getType(codePoint);
        return type == Character.NON_SPACING_MARK
                || type == Character.COMBINING_SPACING_MARK
                || type == Character.ENCLOSING_MARK
                || codePoint >= 0xFE00 && codePoint <= 0xFE0F
                || codePoint >= 0xE0100 && codePoint <= 0xE01EF;
    }

    private static boolean isNoBreakControl(int codePoint) {
        return codePoint == 0x00A0
                || codePoint == 0x2011
                || codePoint == 0x200D
                || codePoint == 0x202F
                || codePoint == 0x2060
                || codePoint == 0xFEFF;
    }

    private static boolean isSmallKana(int codePoint) {
        switch (codePoint) {
            case 0x3041:
            case 0x3043:
            case 0x3045:
            case 0x3047:
            case 0x3049:
            case 0x3063:
            case 0x3083:
            case 0x3085:
            case 0x3087:
            case 0x308E:
            case 0x3095:
            case 0x3096:
            case 0x30A1:
            case 0x30A3:
            case 0x30A5:
            case 0x30A7:
            case 0x30A9:
            case 0x30C3:
            case 0x30E3:
            case 0x30E5:
            case 0x30E7:
            case 0x30EE:
            case 0x30F5:
            case 0x30F6:
                return true;
            default:
                return codePoint >= 0x31F0 && codePoint <= 0x31FF;
        }
    }
}
