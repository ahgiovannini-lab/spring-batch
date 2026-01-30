# Spring Batch Chunk Resume (PostgreSQL)

Microserviço Spring Batch para processar um arquivo de texto enorme em chunks transacionais, garantindo **resume por chunk**: chunks confirmados não são reprocessados, e o chunk interrompido é reprocessado integralmente ao reiniciar.

## Requisitos

- Java 21
- Maven 3.9+
- Docker / Docker Compose

## Como executar

### 1) Subir PostgreSQL

```bash
docker-compose up -d
```

### 2) Gerar arquivo grande (perfil `generate`)

```bash
mvn -q -Dspring-boot.run.profiles=generate spring-boot:run
```

Esse comando gera o arquivo configurado em `batch.inputFile` (por padrão `src/main/resources/input/huge-file.txt`) com milhões de linhas no formato `line-0000000001`.

### 3) Rodar o job

```bash
mvn spring-boot:run
```

O job usa o PostgreSQL para persistir os metadados (JobRepository, ExecutionContext, etc.).

## Como testar o resume por chunk

### Opção A: Interrupção manual (CTRL+C)

1. Ajuste `batch.chunkSize` para um valor pequeno (ex: 1000).
2. Rode o job (`mvn spring-boot:run`).
3. Interrompa com `CTRL+C` durante o processamento de um chunk.
4. Rode novamente (`mvn spring-boot:run`).
5. Observe nos logs:
   - chunks já confirmados **não** são reprocessados;
   - o chunk interrompido é reprocessado **do início**;
   - o job continua do próximo chunk corretamente.

### Opção B: Falha simulada por configuração

No `application.yml`:

```yaml
batch:
  fail:
    enabled: true
    atChunk: 7
```

1. Rode o job (`mvn spring-boot:run`).
2. O job falha no chunk 7 **antes do commit**.
3. Desative a falha (`enabled: false`).
4. Rode novamente.
5. O chunk 7 é reprocessado desde o início e o processamento segue adiante.

## Configurações principais

- `batch.chunkSize`: tamanho do chunk.
- `batch.inputFile`: caminho do arquivo de entrada.
- `batch.fail.enabled`: ativa falha simulada.
- `batch.fail.atChunk`: chunk no qual falhar.
- `spring.batch.jdbc.initialize-schema=always`: cria as tabelas do Spring Batch em dev.

## Observações

- O job não grava as linhas em lugar nenhum; apenas registra log de cada chunk concluído.
- O checkpoint é feito **por chunk**, não por item.
- O `currentIndex` é **apenas interno**: não persistimos `chunkResume.currentIndex` no `ExecutionContext` para evitar qualquer resume por item.
- **Atenção aos JobParameters**: se você reiniciar o app com parâmetros diferentes (mesmo só um timestamp), o Spring cria um novo **JobInstance** e o processamento começa do zero. A validação de restart deve garantir que é o **mesmo JobInstance**.
- **Arquivo/reader determinístico**: se o arquivo for trocado ou mudar de tamanho/ordem entre execuções, o `committedOffset` deixa de apontar para o mesmo conteúdo. Para testes de resume, o arquivo **deve ser exatamente o mesmo** entre runs.

## Comandos úteis

```bash
mvn clean package
mvn spring-boot:run
```
