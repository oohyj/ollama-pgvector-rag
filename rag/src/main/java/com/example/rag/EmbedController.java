package com.example.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmbedController {

    private final RegsEmbedService financeEmbedService;

    @PostMapping("/regs/embed")
    public String embed() {
        financeEmbedService.embedFinanceMarkdown();
        return "OK";

    }
}
