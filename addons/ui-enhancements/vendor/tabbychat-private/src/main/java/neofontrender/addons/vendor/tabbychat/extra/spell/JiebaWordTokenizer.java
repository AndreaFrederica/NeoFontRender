package neofontrender.addons.vendor.tabbychat.extra.spell;

import com.huaban.analysis.jieba.JiebaSegmenter;
import com.huaban.analysis.jieba.SegToken;
import com.swabunga.spell.event.WordTokenizer;

import java.util.ArrayList;
import java.util.List;

/** Supplies precise jieba token offsets to Jazzy's spell-check event pipeline. */
final class JiebaWordTokenizer implements WordTokenizer {
    private static final JiebaSegmenter SEGMENTER = new JiebaSegmenter();

    private final StringBuilder text;
    private List<SegToken> words;
    private int next;
    private SegToken current;

    JiebaWordTokenizer(String text) {
        this.text = new StringBuilder(text == null ? "" : text);
        rebuild();
    }

    @Override public String getContext() { return text.toString(); }
    @Override public int getCurrentWordCount() { return next; }
    @Override public int getCurrentWordEnd() { return current == null ? 0 : current.endOffset; }
    @Override public int getCurrentWordPosition() { return current == null ? 0 : current.startOffset; }
    @Override public boolean isNewSentence() { return false; }
    @Override public boolean hasMoreWords() { return next < words.size(); }

    @Override
    public String nextWord() {
        current = words.get(next++);
        return current.word;
    }

    @Override
    public void replaceWord(String replacement) {
        if (current == null) return;
        int cursor = current.startOffset + (replacement == null ? 0 : replacement.length());
        text.replace(current.startOffset, current.endOffset, replacement == null ? "" : replacement);
        rebuild();
        next = 0;
        while (next < words.size() && words.get(next).endOffset <= cursor) next++;
        current = null;
    }

    private void rebuild() {
        words = new ArrayList<>();
        for (SegToken token : SEGMENTER.process(text.toString(), JiebaSegmenter.SegMode.SEARCH)) {
            if (token.startOffset >= token.endOffset || !containsLetterOrDigit(token.word)) continue;
            words.add(token);
        }
    }

    private static boolean containsLetterOrDigit(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetterOrDigit(value.charAt(i))) return true;
        }
        return false;
    }
}
