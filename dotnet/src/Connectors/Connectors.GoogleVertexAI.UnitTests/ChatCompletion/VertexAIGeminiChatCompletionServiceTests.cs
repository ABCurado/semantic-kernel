﻿// Copyright (c) Microsoft. All rights reserved.

using Microsoft.SemanticKernel.Connectors.GoogleVertexAI;
using Microsoft.SemanticKernel.Services;
using Xunit;

namespace SemanticKernel.Connectors.GoogleVertexAI.UnitTests.ChatCompletion;

public sealed class VertexAIGeminiChatCompletionServiceTests
{
    [Fact]
    public void AttributesShouldContainModelId()
    {
        // Arrange & Act
        string model = "fake-model";
        var service = new VertexAIGeminiChatCompletionService(model, "key", "location", "project");

        // Assert
        Assert.Equal(model, service.Attributes[AIServiceExtensions.ModelIdKey]);
    }
}