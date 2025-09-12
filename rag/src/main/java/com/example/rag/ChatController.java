package com.example.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final RagService ragService;

//    @PostMapping(value = "/chat/stream" , produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public Flux<String> stream(@RequestBody String question){
//        return ragService.streamChat(question);
//    }

    // 새로 추가: data: 없이 플레인 텍스트 스트리밍
    @PostMapping(value = "/chat/stream/plain", produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> streamPlain(@RequestBody String question) {
        return ragService.streamChat(question)
                .bufferTimeout(32, Duration.ofMillis(80))
                .map(chunks -> String.join("", chunks));
    }
}
