package com.example.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RagService {

    private final VectorStore vectorStore;
    private final StreamingChatModel streamingChatModel;

    public Flux<String> streamChat(String question){
        //  1 . 문서 검색 요청 생성
        var searchReq = SearchRequest.builder()
                .query(question)
                .topK(3)
                .build();

        // 2.  블로킹 가능 구간 오프로딩 + 프롬프트 생성 + 스트리밍
        //
        return Mono.fromCallable(() -> vectorStore.similaritySearch(searchReq))
                .subscribeOn(Schedulers.boundedElastic()) // ★ 이벤트루프 보호
                .flatMapMany(docs -> {
                    String template = """
                        당신은 금융 규정집 RAG 도우미입니다.
                        아래 [문서] 내용만 근거로 한국어로 간결하게 답하세요.
                        근거가 없으면 "잘 모르겠습니다."라고 답하세요.
                    
                        질문이 '보호 한도/최대/금액'을 묻는 경우, 정확한 숫자와 단위만 한 줄로 답하세요.
                        예) 5천만 원
                    
                        [문서]
                        ```
                        {documents}
                        ```
                    
                        [질문]
                        {question}
                        """;

                    if (docs == null || docs.isEmpty()) {
                        return Flux.just("잘 모르겠습니다.");
                    }

                    StringBuilder ctx = new StringBuilder();
                    for (Document d : docs) {
                        String title = String.valueOf(d.getMetadata().getOrDefault("title", ""));
                        String body = d.getText();
                        if (body != null && body.length() > 1200) {
                            body = body.substring(0, 1200) + "..."; // 과도한 컨텍스트 방지
                        }
                        ctx.append("### ").append(title).append("\n")
                                .append(body == null ? "" : body).append("\n\n");
                    }

                    Map<String,Object> vars = new HashMap<>();
                    vars.put("documents", ctx.toString());
                    vars.put("question", question);

                    Prompt prompt = new PromptTemplate(template).create(vars);

                    return streamingChatModel.stream(prompt)
                            .mapNotNull(r -> r.getResult().getOutput().getText());
                })
                .onErrorResume(e ->
                        Flux.just("[ERROR] " + e.getClass().getSimpleName() + ": " + e.getMessage()));
    }
}