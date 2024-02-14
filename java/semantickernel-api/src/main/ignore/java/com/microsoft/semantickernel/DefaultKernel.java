// Copyright (c) Microsoft. All rights reserved.
package com.microsoft.semantickernel;

import com.microsoft.semantickernel.ai.AIException;
import com.microsoft.semantickernel.ai.embeddings.TextEmbeddingGeneration;
import com.microsoft.semantickernel.memory.MemoryConfiguration;
import com.microsoft.semantickernel.memory.MemoryStore;
import com.microsoft.semantickernel.memory.NullMemory;
import com.microsoft.semantickernel.memory.SemanticTextMemory;
import com.microsoft.semantickernel.orchestration.*;
import com.microsoft.semantickernel.plugin.Plugin;
import com.microsoft.semantickernel.semanticfunctions.SemanticFunctionConfig;
import com.microsoft.semantickernel.aiservices.AIService;
import com.microsoft.semantickernel.aiservices.AIServiceCollection;
import com.microsoft.semantickernel.aiservices.AIServiceProvider;
import com.microsoft.semantickernel.skilldefinition.DefaultSkillCollection;
import com.microsoft.semantickernel.skilldefinition.ReadOnlyFunctionCollection;
import com.microsoft.semantickernel.skilldefinition.ReadOnlySkillCollection;
import com.microsoft.semantickernel.templateengine.DefaultPromptTemplateEngine;
import com.microsoft.semantickernel.templateengine.PromptTemplateEngine;
import com.microsoft.semantickernel.textcompletion.CompletionKernelFunction;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class DefaultKernel implements Kernel {

    private final KernelConfig kernelConfig;
    private final DefaultSkillCollection defaultSkillCollection;
    private final PromptTemplateEngine promptTemplateEngine;
    private final AIServiceProvider aiServiceProvider;
    private SemanticTextMemory memory;

    @Inject
    public DefaultKernel(
            KernelConfig kernelConfig,
            PromptTemplateEngine promptTemplateEngine,
            @Nullable SemanticTextMemory memoryStore,
            AIServiceProvider aiServiceProvider) {
        if (kernelConfig == null) {
            throw new IllegalArgumentException();
        }

        this.kernelConfig = kernelConfig;
        this.aiServiceProvider = aiServiceProvider;
        this.promptTemplateEngine = promptTemplateEngine;
        this.defaultSkillCollection = new DefaultSkillCollection();

        if (memoryStore != null) {
            this.memory = memoryStore.copy();
        } else {
            this.memory = new NullMemory();
        }
    }

    @Override
    public <T extends AIService> T getService(String serviceId, Class<T> clazz) {

        T service = aiServiceProvider.getService(serviceId, clazz);

        if (service == null) {
            throw new KernelException(
                    KernelException.ErrorCodes.SERVICE_NOT_FOUND,
                    "Service of type "
                            + clazz.getName()
                            + " and name "
                            + serviceId
                            + " not registered");
        } else {
            return service;
        }
    }

    @Override
    public <FunctionType extends KernelFunction> FunctionType registerSemanticFunction(
            FunctionType semanticFunctionDefinition) {
        return null;
    }

    @Override
    public KernelConfig getConfig() {
        return kernelConfig;
    }

    //    @Override
    //    public <RequestConfiguration, FunctionType extends SKFunction>
    //            FunctionType registerSemanticFunction(FunctionType func) {
    //        if (!(func instanceof RegistrableSkFunction)) {
    //            throw new RuntimeException("This function does not implement
    // RegistrableSkFunction");
    //        }
    //        ((RegistrableSkFunction) func).registerOnKernel(this);
    //        defaultSkillCollection.addSemanticFunction(func);
    //        return func;
    //    }

    @Override
    public KernelFunction getFunction(String skill, String function) {
        return defaultSkillCollection.getFunction(skill, function, null);
    }

    @Override
    public CompletionKernelFunction registerSemanticFunction(
            String pluginName, String functionName, SemanticFunctionConfig functionConfig) {
        // Future-proofing the name not to contain special chars
        // Verify.ValidSkillName(skillName);
        // Verify.ValidFunctionName(functionName);

        return SKBuilders.completionFunctions()
                .withSemanticFunctionConfig(functionConfig)
                .withFunctionName(functionName)
                .withPluginName(pluginName)
                .withKernel(this)
                .build();
    }

    /// <summary>
    /// Import a set of functions from the given skill. The functions must have the `SKFunction`
    // attribute.
    /// Once these functions are imported, the prompt templates can use functions to import content
    // at runtime.
    /// </summary>
    /// <param name="skillInstance">Instance of a class containing functions</param>
    /// <param name="skillName">Name of the skill for skill collection and prompt templates. If the
    // value is empty functions are registered in the global namespace.</param>
    /// <returns>A list of all the semantic functions found in the directory, indexed by function
    // name.</returns>
    //    @Override
    //    public ReadOnlyFunctionCollection importSkill(
    //            String skillName, Map<String, SemanticFunctionConfig> skills)
    //            throws SkillsNotFoundException {
    //        skills.entrySet().stream()
    //                .map(
    //                        (entry) -> {
    //                            return SKBuilders.completionFunctions()
    //                                    .withKernel(this)
    //                                    .withSkillName(skillName)
    //                                    .withFunctionName(entry.getKey())
    //                                    .withSemanticFunctionConfig(entry.getValue())
    //                                    .build();
    //                        })
    //                .forEach(this::registerSemanticFunction);
    //
    //        ReadOnlyFunctionCollection collection = getSkill(skillName);
    //        if (collection == null) {
    //            throw new SkillsNotFoundException(ErrorCodes.SKILLS_NOT_FOUND);
    //        }
    //        return collection;
    //    }
    //
    //    @Override
    //    public ReadOnlyFunctionCollection importSkill(
    //            Object skillInstance, @Nullable String skillName) {
    //        if (skillInstance instanceof String) {
    //            throw new KernelException(
    //                    KernelException.ErrorCodes.FUNCTION_NOT_AVAILABLE,
    //                    "Called importSkill with a string argument, it is likely the intention was
    // to"
    //                            + " call importSkillFromDirectory");
    //        }
    //
    //        if (skillName == null || skillName.isEmpty()) {
    //            skillName = ReadOnlySkillCollection.GlobalSkill;
    //        }
    //
    //        // skill = new Dictionary<string, ISKFunction>(StringComparer.OrdinalIgnoreCase);
    //        ReadOnlyFunctionCollection functions =
    //                SkillImporter.importSkill(skillInstance, skillName, () ->
    // defaultSkillCollection);
    //
    //        DefaultSkillCollection newSkills =
    //                functions.getAll().stream()
    //                        .reduce(
    //                                new DefaultSkillCollection(),
    //                                DefaultSkillCollection::addNativeFunction,
    //                                DefaultSkillCollection::merge);
    //
    //        this.defaultSkillCollection.merge(newSkills);
    //
    //        return functions;
    //    }

    @Override
    public ReadOnlySkillCollection getSkills() {
        return defaultSkillCollection;
    }

    @Override
    public ReadOnlyFunctionCollection getSkill() {
        return null;
    }

    @Override
    public CompletionKernelFunction.Builder getSemanticFunctionBuilder() {
        return SKBuilders.completionFunctions().withKernel(this);
    }

    //    @Override
    //    public ReadOnlyFunctionCollection getSkill(String skillName) throws FunctionNotFound {
    //        ReadOnlyFunctionCollection functions =
    // this.defaultSkillCollection.getFunctions(skillName);
    //        if (functions == null) {
    //            throw new FunctionNotFound(FunctionNotFound.ErrorCodes.FUNCTION_NOT_FOUND,
    // skillName);
    //        }
    //
    //        return functions;
    //    }
    //
    //    @Override
    //    public ReadOnlyFunctionCollection importSkillFromDirectory(
    //            String skillName, String parentDirectory, String skillDirectoryName) {
    //        Map<String, SemanticFunctionConfig> skills =
    //                KernelExtensions.importSemanticSkillFromDirectory(
    //                        parentDirectory, skillDirectoryName, promptTemplateEngine);
    //        return importSkill(skillName, skills);
    //    }
    //
    //    @Override
    //    public void importSkillsFromDirectory(String parentDirectory, String... skillNames) {
    //        Arrays.stream(skillNames)
    //                .forEach(
    //                        skill -> {
    //                            importSkillFromDirectory(skill, parentDirectory, skill);
    //                        });
    //    }
    //
    //    @Override
    //    public ReadOnlyFunctionCollection importSkillFromDirectory(
    //            String skillName, String parentDirectory) {
    //        return importSkillFromDirectory(skillName, parentDirectory, skillName);
    //    }
    //
    //    @Override
    //    public ReadOnlyFunctionCollection importSkillFromResources(
    //            String pluginDirectory, String skillName, String functionName) {
    //        return importSkillFromResources(pluginDirectory, skillName, functionName, null);
    //    }
    //
    //    @Override
    //    public ReadOnlyFunctionCollection importSkillFromResources(
    //            String pluginDirectory, String skillName, String functionName, @Nullable Class
    // clazz)
    //            throws KernelException {
    //        Map<String, SemanticFunctionConfig> skills =
    //                KernelExtensions.importSemanticSkillFromResourcesDirectory(
    //                        pluginDirectory, skillName, functionName, clazz,
    // promptTemplateEngine);
    //        return importSkill(skillName, skills);
    //    }

    @Override
    public PromptTemplateEngine getPromptTemplateEngine() {
        return promptTemplateEngine;
    }

    @Override
    public SemanticTextMemory getMemory() {
        return memory;
    }

    public void registerMemory(@Nonnull SemanticTextMemory memory) {
        this.memory = memory;
    }

    @Override
    public Mono<KernelResult> runAsync(KernelFunction... pipeline) {
        return runAsync(SKBuilders.variables().build(), pipeline);
    }

    @Override
    public Mono<KernelResult> runAsync(String input, KernelFunction... pipeline) {
        return runAsync(SKBuilders.variables().withInput(input).build(), pipeline);
    }

    @Override
    public Mono<KernelResult> runAsync(ContextVariables variables, KernelFunction... pipeline) {
        return null;
    }

    @Override
    public Mono<KernelResult> runAsync(
            boolean streaming, ContextVariables variables, KernelFunction... pipeline) {
        // TODO: 1.0 support pipeline

        if (pipeline == null || pipeline.length == 0) {
            throw new SKException("No parameters provided to pipeline");
        }

        List<Mono<FunctionResult>> results = new ArrayList<>();
        for (KernelFunction f : Arrays.asList(pipeline)) {
            results.add(f.invokeAsync(this, variables, streaming));
        }

        return Flux.merge(results).collectList().map(DefaultKernelResult::new);
    }

    public static class Builder implements Kernel.Builder {
        @Nullable private KernelConfig config = null;
        @Nullable private PromptTemplateEngine promptTemplateEngine = null;
        @Nullable private final AIServiceCollection aiServices = new AIServiceCollection();
        private Supplier<SemanticTextMemory> memoryFactory = NullMemory::new;
        private Supplier<MemoryStore> memoryStorageFactory = null;

        /**
         * Set the kernel configuration
         *
         * @param kernelConfig Kernel configuration
         * @return Builder
         */
        public Kernel.Builder withConfiguration(KernelConfig kernelConfig) {
            this.config = kernelConfig;
            return this;
        }

        /**
         * Add prompt template engine to the kernel to be built.
         *
         * @param promptTemplateEngine Prompt template engine to add.
         * @return Updated kernel builder including the prompt template engine.
         */
        public Kernel.Builder withPromptTemplateEngine(PromptTemplateEngine promptTemplateEngine) {
            Verify.notNull(promptTemplateEngine);
            this.promptTemplateEngine = promptTemplateEngine;
            return this;
        }

        /**
         * Add memory storage to the kernel to be built.
         *
         * @param storage Storage to add.
         * @return Updated kernel builder including the memory storage.
         */
        public Kernel.Builder withMemoryStorage(MemoryStore storage) {
            Verify.notNull(storage);
            this.memoryStorageFactory = () -> storage;
            return this;
        }

        /**
         * Add memory storage factory to the kernel.
         *
         * @param factory The storage factory.
         * @return Updated kernel builder including the memory storage.
         */
        public Kernel.Builder withMemoryStorage(Supplier<MemoryStore> factory) {
            Verify.notNull(factory);
            this.memoryStorageFactory = factory::get;
            return this;
        }

        /**
         * Adds an instance to the services collection
         *
         * @param instance The instance.
         * @return The builder.
         */
        public <T extends AIService> Kernel.Builder withDefaultAIService(T instance) {
            Class<T> clazz = (Class<T>) instance.getClass();
            this.aiServices.setService(instance, clazz);
            return this;
        }

        /**
         * Adds an instance to the services collection
         *
         * @param instance The instance.
         * @param clazz The class of the instance.
         * @return The builder.
         */
        public <T extends AIService> Kernel.Builder withDefaultAIService(
                T instance, Class<T> clazz) {
            this.aiServices.setService(instance, clazz);
            return this;
        }

        /**
         * Adds a factory method to the services collection
         *
         * @param factory The factory method that creates the AI service instances of type T.
         * @param clazz The class of the instance.
         */
        public <T extends AIService> Kernel.Builder withDefaultAIService(
                Supplier<T> factory, Class<T> clazz) {
            this.aiServices.setService(factory, clazz);
            return this;
        }

        /**
         * Adds an instance to the services collection
         *
         * @param serviceId The service ID
         * @param instance The instance.
         * @param setAsDefault Optional: set as the default AI service for type T
         * @param clazz The class of the instance.
         */
        public <T extends AIService> Kernel.Builder withAIService(
                @Nullable String serviceId, T instance, boolean setAsDefault, Class<T> clazz) {
            this.aiServices.setService(serviceId, instance, setAsDefault, clazz);

            return this;
        }

        /**
         * Adds a factory method to the services collection
         *
         * @param serviceId The service ID
         * @param factory The factory method that creates the AI service instances of type T.
         * @param setAsDefault Optional: set as the default AI service for type T
         * @param clazz The class of the instance.
         */
        public <T extends AIService> Kernel.Builder withAIServiceFactory(
                @Nullable String serviceId,
                Function<KernelConfig, T> factory,
                boolean setAsDefault,
                Class<T> clazz) {
            this.aiServices.setService(
                    serviceId, (Supplier<T>) () -> factory.apply(this.config), setAsDefault, clazz);
            return this;
        }

        /**
         * Add a semantic text memory entity to the kernel to be built.
         *
         * @param memory Semantic text memory entity to add.
         * @return Updated kernel builder including the semantic text memory entity.
         */
        public Kernel.Builder withMemory(SemanticTextMemory memory) {
            Verify.notNull(memory);
            this.memoryFactory = () -> memory;
            return this;
        }

        /**
         * Add memory storage and an embedding generator to the kernel to be built.
         *
         * @param storage Storage to add.
         * @param embeddingGenerator Embedding generator to add.
         * @return Updated kernel builder including the memory storage and embedding generator.
         */
        public Kernel.Builder withMemoryStorageAndTextEmbeddingGeneration(
                MemoryStore storage, TextEmbeddingGeneration embeddingGenerator) {
            Verify.notNull(storage);
            Verify.notNull(embeddingGenerator);
            this.memoryFactory =
                    () ->
                            SKBuilders.semanticTextMemory()
                                    .withEmbeddingGenerator(embeddingGenerator)
                                    .withStorage(storage)
                                    .build();
            return this;
        }

        @Override
        public Kernel.Builder withPlugins(Plugin... plugins) {
            return null;
        }

        /**
         * Build the kernel
         *
         * @return Kernel
         */
        public Kernel build() {
            if (config == null) {
                config = SKBuilders.kernelConfig().build();
            }

            return build(
                    config,
                    promptTemplateEngine,
                    memoryFactory.get(),
                    memoryStorageFactory == null ? null : memoryStorageFactory.get(),
                    aiServices.build());
        }

        private Kernel build(
                KernelConfig kernelConfig,
                @Nullable PromptTemplateEngine promptTemplateEngine,
                @Nullable SemanticTextMemory memory,
                @Nullable MemoryStore memoryStore,
                @Nullable AIServiceProvider aiServiceProvider) {
            if (promptTemplateEngine == null) {
                promptTemplateEngine = new DefaultPromptTemplateEngine();
            }

            if (kernelConfig == null) {
                throw new AIException(
                        AIException.ErrorCodes.INVALID_CONFIGURATION,
                        "It is required to set a kernelConfig to build a kernel");
            }

            DefaultKernel kernel =
                    new DefaultKernel(
                            kernelConfig, promptTemplateEngine, memory, aiServiceProvider);

            if (memoryStore != null) {
                MemoryConfiguration.useMemory(kernel, memoryStore, null);
            }

            return kernel;
        }
    }
}