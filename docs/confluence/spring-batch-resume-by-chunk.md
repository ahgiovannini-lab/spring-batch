# Spring Batch Resume por Chunk (Checkpointing transacional)

## Problema e motivação
Processar arquivos com milhões de linhas requer controle de transações e retomada (restart) em caso de falhas. O objetivo é garantir **exactly-once por chunk**:

- Chunks **confirmados** (commit) não podem ser reprocessados.
- Se a aplicação cair durante um chunk, ao reiniciar o job deve retomar **do início daquele chunk**.
- O fluxo deve continuar a partir do checkpoint correto.

## Como o Spring Batch persiste metadados
O Spring Batch persiste o estado do job no **JobRepository**, com tabelas como:

- `BATCH_JOB_INSTANCE`
- `BATCH_JOB_EXECUTION`
- `BATCH_STEP_EXECUTION`
- `BATCH_EXECUTION_CONTEXT`

O `ExecutionContext` é usado para armazenar checkpoints do step e é persistido a cada commit de chunk.

## Como o checkpoint por chunk foi implementado

### Estratégia
- Implementamos um `ItemReader` customizado (`ChunkCheckpointingReader`) que também é `ItemStream`.
- O reader mantém um `committedOffset` — o índice **após o último chunk confirmado**.
- Ao reiniciar, o reader abre o arquivo e **pula** diretamente para `committedOffset`.
- O offset só é atualizado **após o chunk ser escrito com sucesso**.

### Componentes principais
1. **ChunkCheckpointingReader**
   - Abre o arquivo.
   - Pula até `committedOffset`.
   - Incrementa `currentIndex` a cada linha lida.
   - Persiste o `committedOffset` apenas no commit do chunk.

2. **ChunkCommitWriter**
   - Não grava dados; apenas marca o `committedOffset` como `currentIndex` do reader.
   - É chamado **antes do commit**, garantindo que o offset persistido representa um chunk completo.

3. **ChunkTrackingListener**
   - Conta chunks confirmados.
   - Loga `Chunk X processed` após cada commit.

### Resultado
- Chunks confirmados não são reprocessados.
- Chunk interrompido é reprocessado do início.

## Como testar

1. Subir PostgreSQL via `docker-compose`.
2. Gerar arquivo grande com o perfil `generate`.
3. Rodar o job com `batch.chunkSize` pequeno.
4. Interromper durante um chunk (CTRL+C) ou usar falha simulada.
5. Reiniciar e confirmar que:
   - chunks anteriores não reprocessam;
   - chunk interrompido é reprocessado integralmente.

## Pitfalls comuns

- **Reader padrão com saveState por item**: pode retomar no meio do chunk, violando a regra de reprocessar o chunk inteiro.
- **Persistir offset por item**: causa reprocessamento parcial e inconsistência.
- **Falta de commit transacional**: sem transação por chunk, o checkpoint não reflete chunks completos.

## Referências
- Spring Batch Reference: JobRepository e ExecutionContext
- Padrão chunk-oriented processing
