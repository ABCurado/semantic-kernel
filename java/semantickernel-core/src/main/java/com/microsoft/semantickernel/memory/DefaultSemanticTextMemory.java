// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel.memory;

import com.microsoft.semantickernel.ai.embeddings.EmbeddingGeneration;
import com.microsoft.semantickernel.exceptions.NotSupportedException;

import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/// <summary>
/// Implementation of <see cref="ISemanticTextMemory"/>./>.
/// </summary>
public class DefaultSemanticTextMemory implements SemanticTextMemory {

    @Nonnull private final EmbeddingGeneration<String, ? extends Number> _embeddingGenerator;
    @Nonnull private /*final*/ MemoryStore _storage;

    public DefaultSemanticTextMemory(
            @Nonnull MemoryStore storage,
            @Nonnull EmbeddingGeneration<String, ? extends Number> embeddingGenerator) {
        this._embeddingGenerator = embeddingGenerator;
        // TODO: this assignment raises EI_EXPOSE_REP2 in spotbugs (filtered out for now)
        this._storage = storage;
    }

    @Override
    public SemanticTextMemory copy() {
        // TODO: this is a shallow copy. Should it be a deep copy?
        return new DefaultSemanticTextMemory(this._storage, this._embeddingGenerator);
    }

    @Override
    public Mono<String> saveInformationAsync(
            @Nonnull String collection,
            @Nonnull String text,
            @Nonnull String id,
            @Nullable String description,
            @Nullable String additionalMetadata) {

        return _embeddingGenerator
                .generateEmbeddingsAsync(Collections.singletonList(text))
                .flatMap(
                        embeddings -> {
                            if (embeddings.isEmpty()) {
                                return Mono.empty();
                            }
                            MemoryRecordMetadata data =
                                    new MemoryRecordMetadata(
                                            true, id, text, description, "", additionalMetadata);
                            MemoryRecord memoryRecord =
                                    new MemoryRecord(
                                            data, embeddings.iterator().next(), collection, null);
                            return _storage.upsertAsync(collection, memoryRecord);
                        });
    }

    @Override
    public Mono<MemoryQueryResult> getAsync(String collection, String key, boolean withEmbedding) {
        return _storage.getAsync(collection, key, withEmbedding)
                .map(record -> new MemoryQueryResult(record.getMetadata(), 1d));
    }

    @Override
    public Mono<Void> removeAsync(@Nonnull String collection, @Nonnull String key) {
        return Mono.error(new NotSupportedException("Pending implementation"));
        //        await this._storage.RemoveAsync(collection, key, cancel);
    }

    private static final Function<
                    Collection<Tuple2<MemoryRecord, Number>>, Mono<List<MemoryQueryResult>>>
            transformMatchesToResults =
                    records -> {
                        if (records.isEmpty()) {
                            return Mono.empty();
                        }
                        return Mono.just(
                                records.stream()
                                        .map(
                                                record -> {
                                                    Tuple2<MemoryRecord, ? extends Number> tuple =
                                                            record;
                                                    MemoryRecord memoryRecord = tuple.getT1();
                                                    Number relevanceScore = tuple.getT2();
                                                    return new MemoryQueryResult(
                                                            memoryRecord.getMetadata(),
                                                            relevanceScore.doubleValue());
                                                })
                                        .collect(Collectors.toList()));
                    };

    @Override
    public Mono<List<MemoryQueryResult>> searchAsync(
            @Nonnull String collection,
            @Nonnull String query,
            int limit,
            double minRelevanceScore,
            boolean withEmbeddings) {

        // TODO: break this up into smaller methods
        return _embeddingGenerator
                .generateEmbeddingsAsync(Collections.singletonList(query))
                .flatMap(
                        embeddings -> {
                            if (embeddings.isEmpty()) {
                                return Mono.empty();
                            }
                            return _storage.getNearestMatchesAsync(
                                            collection,
                                            embeddings.iterator().next(),
                                            limit,
                                            minRelevanceScore,
                                            withEmbeddings)
                                    .flatMap(transformMatchesToResults);
                        });
    }

    @Override
    public Mono<List<String>> getCollectionsAsync() {
        return Mono.error(new NotSupportedException("Pending implementation"));
    }

    @Override
    public Mono<String> saveReferenceAsync(
            @Nonnull String collection,
            @Nonnull String text,
            @Nonnull String externalId,
            @Nonnull String externalSourceName,
            @Nullable String description,
            @Nullable String additionalMetadata) {

        return Mono.error(new NotSupportedException("Pending implementation"));
        //        var embedding = await this._embeddingGenerator.GenerateEmbeddingAsync(text,
        // cancellationToken: cancel);
        //        var data = MemoryRecord.ReferenceRecord(externalId: externalId, sourceName:
        // externalSourceName, description: description,
        //            additionalMetadata: additionalMetadata, embedding: embedding);
        //
        //        if (!(await this._storage.DoesCollectionExistAsync(collection, cancel)))
        //        {
        //            await this._storage.CreateCollectionAsync(collection, cancel);
        //        }
        //
        //        return await this._storage.UpsertAsync(collection, record: data, cancel: cancel);
    }

    @Override
    public SemanticTextMemory merge(MemoryQueryResult b) {
        throw new NotSupportedException("Pending implementation");
    }

    public static class Builder implements SemanticTextMemory.Builder {

        @Nullable MemoryStore storage = null;
        @Nullable EmbeddingGeneration<String, ? extends Number> embeddingGenerator = null;

        @Override
        public Builder setStorage(@Nonnull MemoryStore storage) {
            this.storage = storage;
            return this;
        }

        @Override
        public Builder setEmbeddingGenerator(
                @Nonnull EmbeddingGeneration<String, ? extends Number> embeddingGenerator) {
            this.embeddingGenerator = embeddingGenerator;
            return this;
        }

        @Override
        public SemanticTextMemory build() {
            if (storage == null) {
                throw new IllegalStateException("Storage must be set");
            }
            if (embeddingGenerator == null) {
                throw new IllegalStateException("Embedding generator must be set");
            }
            return new DefaultSemanticTextMemory(storage, embeddingGenerator);
        }
    }
}