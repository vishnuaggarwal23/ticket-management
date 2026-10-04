package com.ticketmanagement.rag;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcVectorChunkStore implements VectorChunkStore {

    private final JdbcTemplate jdbc;

    public JdbcVectorChunkStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void replaceAll(String ticketId, List<StoredChunk> chunks) {
        jdbc.update("DELETE FROM ticket_vector_chunk WHERE ticket_id = ?", ticketId);
        for (StoredChunk chunk : chunks) {
            jdbc.update(
                    """
                            INSERT INTO ticket_vector_chunk
                              (id, ticket_id, chunk_index, content, embedding, metadata, ingested_at)
                            VALUES (?, ?, ?, ?, CAST(? AS vector), CAST(? AS jsonb), ?)
                            """,
                    chunk.id(),
                    chunk.ticketId(),
                    chunk.chunkIndex(),
                    chunk.content(),
                    toVectorLiteral(chunk.embedding()),
                    chunk.metadataJson(),
                    Timestamp.from(chunk.ingestedAt())
            );
        }
    }

    @Override
    public List<StoredChunk> findByTicketId(String ticketId) {
        return jdbc.query(
                """
                        SELECT id, ticket_id, chunk_index, content, embedding::text, metadata::text, ingested_at
                        FROM ticket_vector_chunk
                        WHERE ticket_id = ?
                        ORDER BY chunk_index
                        """,
                (rs, rowNum) -> new StoredChunk(
                        rs.getObject("id", UUID.class),
                        rs.getString("ticket_id"),
                        rs.getInt("chunk_index"),
                        rs.getString("content"),
                        parseVector(rs.getString("embedding")),
                        rs.getString("metadata"),
                        rs.getTimestamp("ingested_at").toInstant()
                ),
                ticketId
        );
    }

    @Override
    public List<RetrievedChunk> searchSimilar(float[] query, int topK, double minSimilarity) {
        String vector = toVectorLiteral(query);
        List<RetrievedChunk> ranked = jdbc.query(
                """
                        SELECT ticket_id, content, 1 - (embedding <=> CAST(? AS vector)) AS similarity
                        FROM ticket_vector_chunk
                        ORDER BY embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """,
                (rs, rowNum) -> new RetrievedChunk(
                        rs.getString("ticket_id"),
                        rs.getString("content"),
                        rs.getDouble("similarity")
                ),
                vector,
                vector,
                topK
        );
        return ranked.stream().filter(chunk -> chunk.similarity() >= minSimilarity).toList();
    }

    static String toVectorLiteral(float[] values) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values[i]);
        }
        return builder.append(']').toString();
    }

    static float[] parseVector(String literal) {
        String trimmed = literal.replace("[", "").replace("]", "").trim();
        if (trimmed.isEmpty()) {
            return new float[0];
        }
        String[] parts = trimmed.split(",");
        float[] values = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            values[i] = Float.parseFloat(parts[i].trim());
        }
        return values;
    }
}
