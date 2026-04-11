package com.wipro.fraud.aiassistant.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.bedrockagentruntime.BedrockAgentRuntimeClient;
import software.amazon.awssdk.services.bedrockagentruntime.model.*;

@Component
@RequiredArgsConstructor
public class BedrockKnowledgeBaseClient {

    private final BedrockAgentRuntimeClient bedrockClient;

    private static final String KNOWLEDGE_BASE_ID = "B6UVYRIGCF";

    private static final String MODEL_ARN =
            "arn:aws:bedrock:eu-north-1:685570573767:inference-profile/eu.amazon.nova-micro-v1:0";

    public String queryKnowledgeBase(String question) {

        RetrieveAndGenerateRequest request =
                RetrieveAndGenerateRequest.builder()

                        .input(RetrieveAndGenerateInput.builder()
                                .text(question)
                                .build())

                        .retrieveAndGenerateConfiguration(
                                RetrieveAndGenerateConfiguration.builder()

                                        .type(RetrieveAndGenerateType.KNOWLEDGE_BASE)

                                        .knowledgeBaseConfiguration(
                                                KnowledgeBaseRetrieveAndGenerateConfiguration.builder()
                                                        .knowledgeBaseId(KNOWLEDGE_BASE_ID)
                                                        .modelArn(MODEL_ARN)
                                                        .build())
                                        .build())

                        .build();

        RetrieveAndGenerateResponse response =
                bedrockClient.retrieveAndGenerate(request);

        return response.output().text();
    }



}