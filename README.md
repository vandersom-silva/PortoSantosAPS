# Gêmeo Digital do Porto de Santos — Módulo Marítimo APS

Aplicação desktop JavaFX para a entrega de Front-end com dados mocados em memória, contemplando **RF001 a RF012** do enunciado: Lineup, berços, catálogo de cargas, frota de navios, marés, programação e histórico de atracações.

## Requisitos para executar

- JDK 25 instalado e configurado como `JAVA_HOME`.
- Apache Maven 3.9 ou superior.
- Conexão com a internet na primeira execução para baixar as dependências JavaFX do Maven Central.

## Executar

Na pasta raiz do projeto:

```bash
mvn clean javafx:run
```

No IntelliJ IDEA, abra a pasta pelo `pom.xml`, selecione o JDK 25 como SDK do projeto e execute o comando acima no terminal integrado ou pelo goal `javafx:run`.

## O que está implementado

- **RF001–RF004:** painel panorâmico do Lineup, KPIs, 11 filtros de perfil, régua de 30 dias com marcadores de maré baixa, cartões de berços, seleção de navio e paginação dinâmica em lotes de cinco (12 páginas para até 60 berços).
- **RF005–RF006:** cadastro, edição, exclusão e auditoria dos berços em `TableView`; a exclusão avisa sobre remoção em cascata das atracações vinculadas.
- **RF007:** catálogo de 15 produtos/commodities, com sentido operacional, perfil e produtividade diária.
- **RF008–RF009:** cadastro e gestão de 160 navios mocados, validação de IMO no formato `IMO-` + sete dígitos, busca textual, edição e exclusão.
- **RF010:** tábua de marés de Novembro, Setembro e Junho de 2026, com 1.440 medições de 30 em 30 minutos por mês, gráfico em `Canvas`, linha crítica de 0,4 m e importação CSV.
- **RF011:** programação de atracações com cálculo de duração estimada, bloqueio de sobreposição no mesmo berço e validação de maré baixa para calado igual ou superior a 13,5 m.
- **RF012:** histórico cronológico pesquisável das 153 atracações mocadas, com inspeção e exclusão; a remoção de registros concluídos exige ativar o perfil simulado de supervisor auditor (RN006).

## Dados e arquitetura

O projeto não acessa banco de dados nem depende de backend. A View JavaFX conversa com `PortController`, que usa `PortService` e a abstração `PortRepository`. A implementação atual é `InMemoryPortRepository`, abastecida por `SantosPortDataFactory`; assim, a origem dos dados pode ser substituída futuramente por um adaptador REST sem acoplar a interface a JDBC ou SQL.

As alterações feitas durante a execução ficam em memória e são reiniciadas ao fechar a aplicação. Os dados são demonstrativos, não representam telemetria oficial do Porto de Santos.

### Importação da tábua CSV

A tela **Marés** aceita arquivos CSV com exatamente 1.440 medições ordenadas em intervalos consecutivos de 30 minutos para o mês selecionado. Formatos aceitos:

```csv
2026-11-01;00:00;0,62
2026-11-01;00:30;0,71
```

ou

```csv
2026-11-01T00:00;0.62
2026-11-01T00:30;0.71
```

O primeiro formato aceita altura decimal com vírgula porque usa ponto e vírgula como separador de colunas. Cabeçalho na primeira linha é ignorado.

## Estrutura resumida

```text
src/main/java/br/edu/unirv/portos/
  PortosApplication.java
  controller/PortController.java
  model/                         # Berth, RegisteredVessel, ShipSchedule, TideMeasurement...
  repository/                    # PortRepository, InMemoryPortRepository
  service/                       # PortService, SantosPortDataFactory
  view/                           # MainView e telas RF001–RF012
src/main/resources/css/app.css
```


### Movimentação de navios no Lineup

Na tela Lineup, pressione sobre uma barra de navio e arraste até o berço e dia de início desejados. A prévia acompanha o ponteiro em tempo real, sem alterar os dados antes de soltar: verde indica destino válido e vermelho indica bloqueio. A duração original da atracação é preservada. Ao soltar, o sistema valida calado máximo do berço, perfil de carga, sobreposição e maré baixa. Pela RN002, navios com calado igual ou superior a 13,5 m são impedidos de permanecer em dias com maré igual ou inferior a 0,4 m. Navios com calado abaixo de 13,5 m podem operar nesses dias conforme o requisito. A mesma validação é aplicada ao cadastrar/mover atracações, atualizar o calado de um navio e importar a tábua de novembro. Se alguma regra falhar, a movimentação/importação é cancelada e uma mensagem explica o motivo.
