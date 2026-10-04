package com.ticketmanagement.rag;

import com.ticketmanagement.config.RagProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TicketChunker {

    private static final Pattern COMMENT_LINE = Pattern.compile("^- \\[(.+?)] (.*)$", Pattern.MULTILINE);

    private final RagProperties ragProperties;

    public TicketChunker(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<TextChunk> chunk(KnowledgeDocument document) {
        if (!document.embeddable()) {
            return List.of();
        }
        RagProperties.Chunking cfg = ragProperties.chunking();
        List<SectionBlock> blocks = new ArrayList<>();
        blocks.addAll(headerBlocks(document.assembledText()));
        blocks.addAll(descriptionBlocks(document.assembledText()));
        blocks.addAll(commentBlocks(document.assembledText()));
        blocks.addAll(resolutionBlocks(document.assembledText()));
        List<TextChunk> chunks = new ArrayList<>();
        int index = 0;
        InstantSnapshot snapshot = InstantSnapshot.from(document);
        for (SectionBlock block : blocks) {
            for (String piece : expand(block.text(), block.mergeable(), cfg)) {
                if (piece.isBlank()) {
                    continue;
                }
                chunks.add(new TextChunk(
                        index,
                        piece,
                        new RagChunkMetadata(
                                snapshot.ticketId(),
                                snapshot.metadata().status(),
                                snapshot.metadata().priority(),
                                snapshot.metadata().assignee(),
                                snapshot.metadata().category(),
                                index,
                                snapshot.metadata().ingestedAt()
                        )));
                index++;
            }
        }
        return chunks;
    }

    private static List<String> expand(String text, boolean mergeable, RagProperties.Chunking cfg) {
        List<String> parts = mergeable ? splitParagraphs(text) : List.of(text);
        List<String> overflow = new ArrayList<>();
        for (String part : parts) {
            overflow.addAll(splitOverflow(part, cfg.maxChars(), cfg.overlapChars()));
        }
        if (!mergeable) {
            return overflow;
        }
        return mergeSmall(overflow, cfg.minChars(), cfg.maxChars());
    }

    private static List<SectionBlock> headerBlocks(String assembled) {
        int descriptionStart = assembled.indexOf("Description:");
        if (descriptionStart < 0) {
            return List.of();
        }
        String header = assembled.substring(0, descriptionStart).trim();
        if (header.isBlank()) {
            return List.of();
        }
        return List.of(new SectionBlock(header, false));
    }

    private static List<SectionBlock> descriptionBlocks(String assembled) {
        String body = section(assembled, "Description:", "Comments:");
        if (body.isBlank()) {
            return List.of();
        }
        return List.of(new SectionBlock(body, true));
    }

    private static List<SectionBlock> commentBlocks(String assembled) {
        String body = section(assembled, "Comments:", "Resolution:");
        if (body.isBlank() || body.equals("(none)")) {
            return List.of();
        }
        List<SectionBlock> blocks = new ArrayList<>();
        Matcher matcher = COMMENT_LINE.matcher(body);
        while (matcher.find()) {
            blocks.add(new SectionBlock("- [" + matcher.group(1) + "] " + matcher.group(2), false));
        }
        return blocks;
    }

    private static List<SectionBlock> resolutionBlocks(String assembled) {
        String body = sectionAfter(assembled, "Resolution:");
        if (body.isBlank() || body.equals("(none)")) {
            return List.of();
        }
        return List.of(new SectionBlock(body, true));
    }

    private static String section(String assembled, String startLabel, String endLabel) {
        int start = assembled.indexOf(startLabel);
        int end = assembled.indexOf(endLabel);
        if (start < 0 || end < 0 || end <= start) {
            return "";
        }
        return assembled.substring(start + startLabel.length(), end).trim();
    }

    private static String sectionAfter(String assembled, String startLabel) {
        int start = assembled.indexOf(startLabel);
        if (start < 0) {
            return "";
        }
        return assembled.substring(start + startLabel.length()).trim();
    }

    private static List<String> splitParagraphs(String text) {
        String[] raw = text.split("\\n\\s*\\n");
        List<String> parts = new ArrayList<>();
        for (String part : raw) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                parts.add(trimmed);
            }
        }
        return parts.isEmpty() ? List.of(text.trim()) : parts;
    }

    static List<String> splitOverflow(String block, int maxChars, int overlapChars) {
        if (block.length() <= maxChars) {
            return List.of(block);
        }
        int overlap = Math.max(0, Math.min(overlapChars, maxChars - 1));
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (start < block.length()) {
            int end = Math.min(start + maxChars, block.length());
            pieces.add(block.substring(start, end));
            if (end == block.length()) {
                break;
            }
            start = end - overlap;
        }
        return pieces;
    }

    static List<String> mergeSmall(List<String> parts, int minChars, int maxChars) {
        if (parts.size() <= 1) {
            return parts;
        }
        List<String> merged = new ArrayList<>();
        StringBuilder current = new StringBuilder(parts.getFirst());
        for (int i = 1; i < parts.size(); i++) {
            String next = parts.get(i);
            if (current.length() < minChars && current.length() + 1 + next.length() <= maxChars) {
                current.append('\n').append(next);
            } else {
                merged.add(current.toString());
                current = new StringBuilder(next);
            }
        }
        merged.add(current.toString());
        return merged;
    }

    private record SectionBlock(String text, boolean mergeable) {
    }

    private record InstantSnapshot(String ticketId, RagChunkMetadata metadata) {
        static InstantSnapshot from(KnowledgeDocument document) {
            return new InstantSnapshot(document.ticketId(), document.metadataSnapshot());
        }
    }
}
