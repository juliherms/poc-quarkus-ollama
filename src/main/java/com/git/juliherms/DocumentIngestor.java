package com.git.juliherms;

import static dev.langchain4j.data.document.splitter.DocumentSplitters.recursive;

import java.nio.file.Paths;
import java.util.List;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import io.quarkiverse.langchain4j.pgvector.PgVectorEmbeddingStore;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Responsável por ingerir documentos no pgvector para uso em RAG.
 * O EmbeddingModel (text-embedding-3-small) e o PgVectorEmbeddingStore
 * são fornecidos automaticamente pelas extensões quarkus-langchain4j-openai
 * e quarkus-langchain4j-pgvector.
 */
@ApplicationScoped
public class DocumentIngestor {

    @Inject
    PgVectorEmbeddingStore embeddingStore;

    @Inject
    EmbeddingModel embeddingModel;

    /**
     * Executado na inicialização da aplicação: carrega o documento
     * "pacotes-viagem.md" do disco e delega para ingest() a geração
     * dos embeddings e a gravação no pgvector.
     */
    public void onStart(@Observes StartupEvent event) {
        Document document = FileSystemDocumentLoader.loadDocument(
            Paths.get("src/main/resources/rag/pacotes-viagem.md")
        );

        document.metadata().put("type", "packages");

        List<Document> documents = List.of(document);

        ingest(documents);
    }

    /**
     * Ingere uma lista de documentos no vector store.
     * Cada documento é dividido em chunks de 500 tokens sem sobreposição
     * e vetorizado com o modelo text-embedding-3-small da OpenAI.
     */
    public void ingest(List<Document> documents) {
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .documentSplitter(recursive(500, 50))
                .build();
        ingestor.ingest(documents);
    }
}
