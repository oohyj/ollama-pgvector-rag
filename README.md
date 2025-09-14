# Spring AI + Ollama + PgVector RAG Chatbot

## 프로젝트 소개
Spring AI, Ollama, PostgreSQL(pgvector)을 활용해 자체 데이터를 임베딩하여 검색하고,  
StreamingChatModel을 통해 실시간 스트리밍 응답을 제공하는 간단한 **RAG 챗봇**입니다.

---

## 사용 기술
- **Backend**: Java 17, Spring Boot 3.4.7, (WebFlux) Spring AI  
- **Model Serving**: Ollama (`nomic-embed-text`, `gemma:2b-instruct`)  
- **Database**: PostgreSQL + pgvector (768차원 벡터 저장/검색)  
- **Infra**: Docker, Gradle  

---

## 아키텍처
1. **문서 임베딩 파이프라인**
   - Markdown 문서를 `###` 섹션 단위로 파싱  
   - Ollama Embedding API(`nomic-embed-text`) 호출 → 벡터 생성  
   - PgVector(PostgreSQL)에 저장  

2. **질문 처리**
   - 사용자 질문 입력 시 → `similaritySearch(topK=3)` 실행  
   - 유사도가 높은 문서 반환  

3. **응답 생성**
   - 검색된 문서를 기반으로 프롬프트 구성  
   - Ollama Chat Model(`gemma:2b-instruct`) 호출  
   - 응답을 토큰 단위로 스트리밍(Flux)  

---

## 주요 기능
- **데이터 임베딩 및 저장**
  - Markdown 문서를 섹션(`###`) 단위로 분리 → Document 객체 변환  
  - Ollama 임베딩 모델을 통해 벡터화 후 pgvector에 저장  

- **질문 검색 & 답변 생성**
  - pgvector `similaritySearch` 실행  
  - 검색된 문서를 기반으로 답변 생성  
  - 토큰 단위 스트리밍 응답 (터미널 `curl`에서 실시간 확인 가능)  

- **API**
  - `POST /regs/embed` → 문서 임베딩 및 벡터 DB 저장   
  - `POST /chat/stream/plain` → 일반 텍스트 스트리밍 응답  
