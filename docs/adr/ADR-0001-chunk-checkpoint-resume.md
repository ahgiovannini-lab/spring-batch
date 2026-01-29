# ADR-0001: Checkpoint e resume por chunk no Spring Batch

## Contexto
Precisamos processar um arquivo com milhões de linhas usando Spring Batch, garantindo:

- chunks confirmados não sejam reprocessados;
- chunk interrompido seja reprocessado do início ao reiniciar;
- continuidade correta após restart.

Além disso, os metadados do batch devem ser persistidos no PostgreSQL via JobRepository.

## Decisão
Implementar um `ItemReader` customizado (`ChunkCheckpointingReader`) que é `ItemStream` e mantém um `committedOffset` (posição da próxima linha após o último chunk confirmado). O offset só é persistido no `ExecutionContext` no momento do commit do chunk. Um `ItemWriter` vazio (`ChunkCommitWriter`) marca o offset como `currentIndex` do reader antes do commit. Com isso, ao reiniciar, o reader pula diretamente para o offset do último chunk confirmado, reprocessando integralmente o chunk interrompido.

## Alternativas consideradas

1. **Salvar offset por item**
   - Prós: restart mais granular.
   - Contras: retoma no meio do chunk, reprocessando parcialmente e violando a regra de reprocessar o chunk completo.

2. **Tornar o writer idempotente e permitir reprocessar chunks**
   - Prós: simplifica reader.
   - Contras: não atende ao requisito de não reprocessar chunks já confirmados.

3. **Persistir itens em staging table e retomar via consultas**
   - Prós: controle fino de estado.
   - Contras: aumenta complexidade e custo operacional.

## Consequências e trade-offs

- **Pró**: garante exactly-once por chunk com comportamento previsível em restart.
- **Pró**: simples de operar e validar via logs.
- **Contra**: o reader precisa fazer skip até o offset no restart, o que pode ser custoso para arquivos muito grandes.
- **Contra**: exige disciplina para não persistir estado por item.

## Como operar e testar

1. Subir PostgreSQL com `docker-compose`.
2. Gerar arquivo grande via perfil `generate`.
3. Rodar o job com chunk pequeno.
4. Forçar falha (CTRL+C ou `batch.fail.enabled=true` e `batch.fail.atChunk=7`).
5. Reiniciar e validar que o chunk interrompido reprocessa integralmente e chunks anteriores não reprocessam.
