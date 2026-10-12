CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE article_categories (
    id UUID PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE
);
CREATE TABLE articles (
    id UUID PRIMARY KEY,
    category_id UUID REFERENCES article_categories(id),
    title VARCHAR(512) NOT NULL,
    content TEXT NOT NULL,
    search_vector TSVECTOR GENERATED ALWAYS AS (
        setweight(to_tsvector('russian', title), 'A') || setweight(to_tsvector('russian', content), 'B')
    ) STORED,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_articles_search ON articles USING GIN(search_vector);
CREATE INDEX idx_articles_category ON articles(category_id);
CREATE TABLE article_embeddings (
    id UUID PRIMARY KEY,
    article_id UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL CHECK (chunk_index >= 0),
    chunk_text TEXT NOT NULL,
    embedding VECTOR(768) NOT NULL,
    model VARCHAR(128) NOT NULL,
    UNIQUE(article_id, chunk_index)
);
-- HNSW works on an initially empty knowledge base without IVFFlat training data.
CREATE INDEX idx_article_embeddings_vector ON article_embeddings USING hnsw (embedding vector_cosine_ops);
