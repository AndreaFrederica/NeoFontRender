package neofontrender.addons.vendor.tabbychat.extra.spell;

import com.huaban.analysis.jieba.WordDictionary;
import com.swabunga.spell.engine.SpellDictionary;

import java.util.Collections;
import java.util.List;

/** Adapts jieba-analysis' bundled Chinese word frequencies to Jazzy. */
final class JiebaSpellDictionary implements SpellDictionary {
    private final WordDictionary dictionary = WordDictionary.getInstance();

    @Override
    public boolean addWord(String word) {
        // Runtime additions are handled by Jazzy's separate user dictionary.
        return false;
    }

    @Override
    public boolean isCorrect(String word) {
        return word != null && containsHan(word) && dictionary.containsWord(word);
    }

    @Override
    public List getSuggestions(String word, int threshold) {
        return Collections.emptyList();
    }

    @Override
    public List getSuggestions(String word, int threshold, int[][] matrix) {
        return Collections.emptyList();
    }

    private static boolean containsHan(String word) {
        for (int i = 0; i < word.length(); i++) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(word.charAt(i));
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS) return true;
        }
        return false;
    }
}
