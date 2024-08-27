# Atlassian RAG + Scraper

> Atlassian의 제품군(Jira, Confluence, Bitbucket, etc ..)에 대한 Q&A 데이터를 수집,
> 
> 제품 사용 중 문제가 발생할 경우, 유사한 데이터를 찾아 보다 높은 정확도의 답변을 제공하는 어플리케이션입니다.

---

## Getting Started

### Clone Repo
```bash
git clone https://github.com/youngmoneee/atlassian-rag.git && cd atlassian-rag
```

### Vector Database

- 해당 어플리케이션은 개발 및 테스트 목적으로 Docker-compose로 구동되는 Weaviate DB를 사용합니다.

- 프로덕션 환경에서 사용할 벡터 데이터베이스가 있다면, 아래 설정을 통해 구성을 변경해주세요.

https://github.com/youngmoneee/atlassian-rag/blob/5953165b3475148c2fc32c54e9f1fbc9672811b6/src/main/resources/application.yml#L18-L20

- 프로퍼티 설정과 관련해 참고 가능한 문서는 아래와 같습니다.
    > [Vector DB 공식문서](https://docs.spring.io/spring-ai/reference/api/vectordbs.html#_available_implementations)

### Scrape

- 어플리케이션 실행 시 `scrape` 또는 `-–scrape` 인자를 전달하면, ETL 파이프라인을 통해 문서 수집을 시작합니다.
  
  ![etl](https://github.com/user-attachments/assets/51fcd8bf-9a0b-4d07-a664-56bd4159996c)
  
- 기본적으로 Stackoverflow와 아틀라시안 커뮤니티에 대한 DocumentReader가 구현되어있습니다.
- 수집, 변환 등 추가적인 작업이 필요할 경우 아래 인터페이스를 구현해 확장할 수 있습니다.

>  1. **DocumentReader** 구현
>
>     - https://github.com/youngmoneee/atlassian-rag/blob/19de44b7b266de60e884e02361ac4525d8d8496a/src/main/java/io/ymon/rag/document/DocumentReader.java#L7
>
>     - **❗️주의 사항**
>
>       - 해당 레포지토리에는 `text-embedding-3-small`을 통해 얻은 임베딩 값을 포함한 Document 객체와, Document로 변환하기 전의 `QnA` 객체의 샘플 데이터가 포함되어있습니다. 서버에 요청을 보내기 전 **충분히** 테스트하세요.
>         ```java
>         public class SampleDocumentReader {
>           @Resource("classpath:/document_sample.jsonl")
>           private final Resource sampleDocumentDatas;
>           @Resource("classpath:/qna_sample.jsonl")
>           private final Resource smapleQnaDatas;
>           ...
>         }
>         ```
>         
>       - 아틀라시안 커뮤니티의 robots.txt에 따르면 요청 사이의 간격은 5초입니다.
>         ```java
>         .flatMap(documentation)
>         .delayElements(Duration.ofSeconds(5));
>         ```
>         
>         요청 사이에 적절한 딜레이를 주어 서버에 과도한 부하가 걸리지 않도록 주의하세요.
>
>       - ⚠️ 무분별한 요청 시, 서버에서 DDOS 공격이라 판단해 차단당할 수 있습니다. (이런 경험은 저만 하겠습니다..)
> 
>  2. **DocumentTransformer** 구현
>
>     - 기본적으로 OpenAi를 사용중이나, 프로퍼티 설정을 통해 [임베딩 모델](https://docs.spring.io/spring-ai/reference/api/embeddings.html#available-implementations)을 변경할 수 있습니다.
>
>     - VectorStore 구현체에서 임베딩 값이 없는 객체에 임베딩 작업을 수행하나, 동기적 메커니즘으로 인해 병목이 발생할 수 있습니다. 구현되어있는 `EmbeddingDocumentTransformer`를 사용하세요. 
>
>     - 임베딩 외에도 DocumentTransformer를 구현해 Document의 메타데이터 등을 일괄적으로 제어할 수 있습니다.
>    
>  3. **DocumentWriter** 구현
>
>     - 구성된 [Vector Store](https://github.com/youngmoneee/atlassian-rag/edit/main/README.md#vector-database)가 있다면, 해당 데이터베이스에 Document 객체를 삽입합니다.
>
>     - DocumentWriter의 구현체가 없는 경우, LoggerWriter가 주입되며 콘솔에서 작업이 완료된 Document 객체를 확인할 수 있습니다.

---
### Retriever

> Knowledge Base가 구축이 되었다면, Retriever로 사용자의 질문과 유사한 Document를 찾을 수 있습니다.
> 
> 아래 이미지는 Retriever가 Knowledge Base에서 사용자 질문과 유사한 문서를 찾는 과정을 설명합니다.
> 
> **Concept**
> 
> <img width="747" alt="rag_flow" src="https://github.com/user-attachments/assets/8bc08a5f-6efc-47c5-ac70-da39721049c3">

---
#### ChatClient(Generator)
해당 어플리케이션은 응답의 생성을 위해 ChatClient를 필요로 합니다.
https://github.com/youngmoneee/atlassian-rag/blob/ff862bf77f8252ab2604f566a355843f5c2bd0f2/src/main/resources/application.yml#L7-L12

[ChatModel](https://docs.spring.io/spring-ai/reference/api/chatmodel.html#_available_implementations)를 수정해, 답변 생성에 사용할 모델을 지정할 수 있습니다.

> **⚠️주의**
> 
> 응답을 생성하는 ChatModel과 별개로, 유사도 검색에 사용되는 Embedding Model은 EmbeddingTransformer에서 사용한 모델과 **동일**해야합니다.
> 
> 임베딩 모델이 동일하지 않다면 유사도 검색의 정확도가 떨어질 수 있으니 Knowledge Base 구축에 사용한 Embedding 모델을 사용해 주세요.

---
#### RequestResponseAdvisor

> 해당 인터페이스는 Request, Response를 중간에 가로채 추가적인 동작을 수행합니다(Logging, Add Context, Add Chat history, etc ..)
>
> ##### Parameter
> 
> AdvisedRequest에는 크게 UserParams와 AdvisorParams라는 두 가지 컨텍스트가 존재합니다.
>
> 1. **UserParams**
> 
> 예시 코드:
> ```java
> "hi, my name is {name}."
> ```
> 위와 같은 userText가 존재할 때, **렌더링 과정**에서 UserParams으로부터 "name"을 조회하고, 해당 PlaceHolder를 대체하게 됩니다.
>
> 2. **AdvisorParams**
> 
> 예시 코드:
> ```java
> var req = chatClient.prompt().advisors(advisorSpec -> advisorSpec
>   .param(USER_UUID, xxxx)
>   ...
>
> // in Advisors
> Map<String, Object> advisorParams = request.advisorParams();
> String userUuid = (String) advisorParams.get(USER_UUID);
> ...
> ```
> RequestResponseAdvisor 구현체는, advisor 작업에 필요한 정보를 AdvisorParams로부터 꺼내어 사용하게 됩니다.
>
> Request, Response 객체에 추가적인 제어가 필요할 경우 해당 인터페이스를 구현하세요.

---
## Test page

> 간단한 테스트를 위한 static 페이지를 포함하고 있습니다.
>
> 어플리케이션 실행 후 root 경로를 통해 접근 가능하며, 간단한 질문을 하고 받은 응답을 렌더링하여 볼 수 있습니다.
> 
> <img width="240" alt="Screenshot 2024-08-27 at 7 14 22 PM" src="https://github.com/user-attachments/assets/f9bff4b2-1f5e-40db-b3b4-fd382f7a529a">

js의 marked 라이브러리가 code 태그를 렌더링 할 때, 가끔 정상적으로 변환되지 않는 문제가 있어 원본 마크다운을 동시에 출력합니다.

---
## Build & Test & Deploy

### 빌드

```bash
./gradlew clean build
```

### 테스트

```bash
./gradlew test
```

### 배포

```bash
# for Jar
./gradlew bootJar
# for Container(필요 시 jib 사용)
./gradlew bootBuildImage
```
