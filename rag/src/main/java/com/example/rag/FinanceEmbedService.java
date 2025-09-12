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
public class FinanceEmbedService {

    private final VectorStore vectorStore;
    private final ResourceLoader loader;

    /**
     * finance.md를 읽어 섹션(###) 단위로 Document로 만들고
     * Ollama 임베딩 + PgVector 저장을 실행한다.
     */
    public Mono<Integer> embedFinanceMarkdown() {
        return Mono.fromCallable(() -> {
            Resource res = loader.getResource("classpath:regs/regs.md");
            String md = new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            List<Document> docs = new ArrayList<>();
            for (String block : md.split("(?m)^###\\s+")) {
                String t = block.trim();
                if (t.isEmpty()) continue;
                String[] lines = t.split("\\R", 2);
                String title = lines[0].trim();
                String content = (lines.length > 1 ? lines[1].trim() : "");
                Map<String, Object> meta = Map.of("title", title, "source", "finance.md");
                docs.add(new Document(content, meta));
            }

            if (!docs.isEmpty()) {
                // 내부적으로 Ollama 임베딩 호출이 블로킹일 수 있으므로 워커 스레드에서 실행
                vectorStore.add(docs);
            }
            return docs.size();
        }).subscribeOn(Schedulers.boundedElastic()); // ★ 이벤트루프 보호
    }
}