package mnm.mods.tabbychat.extra.spell;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JiebaWordTokenizerTest {
    @Test
    void segmentsChineseWithOffsetsAndKeepsMixedEnglish() {
        String text = "测试聊天窗口 hello";
        JiebaWordTokenizer tokenizer = new JiebaWordTokenizer(text);
        List<String> words = new ArrayList<>();
        List<Integer> starts = new ArrayList<>();
        while (tokenizer.hasMoreWords()) {
            words.add(tokenizer.nextWord());
            starts.add(tokenizer.getCurrentWordPosition());
        }

        assertEquals(List.of("测试", "聊天", "窗口", "hello"), words);
        assertEquals(List.of(0, 2, 4, 7), starts);
    }

    @Test
    void usesJiebaDictionaryForChineseOnly() {
        JiebaSpellDictionary dictionary = new JiebaSpellDictionary();
        assertTrue(dictionary.isCorrect("聊天"));
        assertFalse(dictionary.isCorrect("hello"));
        assertFalse(dictionary.isCorrect("聊天窗口不存在词条组合"));
    }
}
