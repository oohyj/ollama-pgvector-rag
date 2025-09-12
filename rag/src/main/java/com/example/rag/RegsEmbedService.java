package com.example.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegsEmbedService {

    private final VectorStore vectorStore;
    private final ResourceLoader loader;

    /**
     * regs.md를 읽어 ### 단위로 Document로 만들고
     * Ollama 임베딩 + PgVector 저장을 실행
     */
    public Mono<Integer> embedFinanceMarkdown() { // 비동기 실행
        return Mono.fromCallable(() -> { // 리액터 체인 안에 넣기 위해 블로킹 코드를 mono로 감싼다.
            Resource res = loader.getResource("classpath:regs/regs.md"); // 리소스 불러오기
            String md = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            List<Document> docs = new ArrayList<>();
            for (String block : md.split("(?m)^###\\s+")) { // ### <- 섹션 단위별로 자르기
                String t = block.trim();
                if (t.isEmpty()) continue;
                String[] lines = t.split("\\R", 2); // 제목 , 본문으로 나누기 , 줄 바꿈 기준으로 나눔
                String title = lines[0].trim();
                String content = (lines.length > 1 ? lines[1].trim() : "");
                Map<String, Object> meta = Map.of("title", title, "source", "regs.md");
                docs.add(new Document(content, meta)); // 본문이랑 메타데이터 묶어서 객체 생성
            }

            if (!docs.isEmpty()) {
                // Ollama 임베딩 호출이 블로킹일 수 있으므로 워커 스레드에서 실행
                vectorStore.add(docs);
            }
            return docs.size();
        }).subscribeOn(Schedulers.boundedElastic()); // 블로킹 코드를 전용 워커 스레드 풀에서 실행 (boundedElastic)
    }
}