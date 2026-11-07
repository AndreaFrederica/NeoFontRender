package neofontrender.text.pipeline;

import neofontrender.text.StructuredText;
import neofontrender.text.syntax.TextSyntaxEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Executes syntax once and then all registered structural middleware deterministically. */
public final class StructuredTextPipeline {
    private final TextSyntaxEngine syntax;
    private final List<StructuredTextMiddleware> middleware;
    private final List<LineBreakOpportunityProvider> lineBreakProviders;

    public StructuredTextPipeline(TextSyntaxEngine syntax,
                                  Collection<? extends StructuredTextMiddleware> middleware) {
        this(syntax, middleware, Collections.emptyList());
    }

    public StructuredTextPipeline(TextSyntaxEngine syntax,
                                  Collection<? extends StructuredTextMiddleware> middleware,
                                  Collection<? extends LineBreakOpportunityProvider> lineBreakProviders) {
        this.syntax = Objects.requireNonNull(syntax, "syntax");
        List<StructuredTextMiddleware> ordered = new ArrayList<>();
        if (middleware != null) ordered.addAll(middleware);
        ordered.sort(Comparator.comparingInt(StructuredTextMiddleware::priority).reversed()
                .thenComparing(StructuredTextMiddleware::id));
        this.middleware = Collections.unmodifiableList(ordered);
        List<LineBreakOpportunityProvider> breaks = new ArrayList<>();
        if (lineBreakProviders != null) breaks.addAll(lineBreakProviders);
        breaks.sort(Comparator.comparingInt(LineBreakOpportunityProvider::priority).reversed()
                .thenComparing(LineBreakOpportunityProvider::id));
        this.lineBreakProviders = Collections.unmodifiableList(breaks);
    }

    public StructuredText parse(String source) {
        return process(syntax.parse(source));
    }

    public StructuredText process(StructuredText parsed) {
        StructuredText current = Objects.requireNonNull(parsed, "parsed");
        for (StructuredTextMiddleware component : middleware) {
            if (!component.isEnabled()) continue;
            StructuredText next = component.process(current);
            if (next != null) current = next;
        }
        return current;
    }

    public List<String> middlewareIds() {
        List<String> ids = new ArrayList<>(middleware.size());
        for (StructuredTextMiddleware value : middleware) ids.add(value.id());
        return Collections.unmodifiableList(ids);
    }

    public List<Integer> breakOpportunities(StructuredText text) {
        java.util.TreeSet<Integer> result = new java.util.TreeSet<>();
        for (LineBreakOpportunityProvider provider : lineBreakProviders) {
            if (provider.isEnabled()) result.addAll(provider.opportunities(text));
        }
        return Collections.unmodifiableList(new ArrayList<>(result));
    }

    public List<String> lineBreakProviderHits(StructuredText text) {
        List<String> ids = new ArrayList<>();
        for (LineBreakOpportunityProvider provider : lineBreakProviders) {
            if (provider.isEnabled() && !provider.opportunities(text).isEmpty()) ids.add(provider.id());
        }
        return Collections.unmodifiableList(ids);
    }

    public List<String> lineBreakProviderIds() {
        List<String> ids = new ArrayList<>(lineBreakProviders.size());
        for (LineBreakOpportunityProvider value : lineBreakProviders) ids.add(value.id());
        return Collections.unmodifiableList(ids);
    }
}
