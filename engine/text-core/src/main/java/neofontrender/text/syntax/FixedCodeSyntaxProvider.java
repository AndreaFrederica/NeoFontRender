package neofontrender.text.syntax;

import java.util.Set;

/** Marker used to reject ambiguous ownership of fixed two-character controls. */
public interface FixedCodeSyntaxProvider extends TextSyntaxProvider {
    Set<Character> codes();
}
