# RadarSptrans

## 📌 Visão Geral
O **RadarSptrans** é uma API Spring Boot que combina dois dados públicos da SPTrans para localizar ônibus na cidade de São Paulo e estimar quanto tempo se espera por eles:

- **API Olho Vivo** (tempo real): linhas, posição dos veículos e previsão de chegada.
- **GTFS** (programado): todas as ~22 mil paradas, o itinerário completo de cada linha e o intervalo programado entre ônibus por faixa horária.

O projeto segue arquitetura hexagonal e inclui um mapa com Leaflet servido pela própria aplicação.

## 🧱 Arquitetura
- **Domínio** (`domain`): modelos próprios (`Linha`, `Veiculo`, `Parada`, `Itinerario`, `TempoEsperaParada`...), portas e a lógica pura de cálculo (`CalculadoraTempoEspera`, `Distancia`). Os nomes da SPTrans (`cl`, `py`, `px`...) não saem dos adaptadores.
- **Aplicação** (`application`): `SpTransApplicationService` (linhas e posições), `ParadaApplicationService` (paradas próximas e tempo de espera) e `SessaoSpTrans`, que reaproveita o cookie de sessão e refaz o login uma vez quando a SPTrans o recusa.
- **Adaptadores**:
  - `SpTransAuthClientAdapter` / `SpTransClientAdapter`: API Olho Vivo via OpenFeign, com cache (Caffeine) e uma nova tentativa em falhas de rede nas consultas.
  - `GtfsAdapter`: baixa o GTFS em segundo plano ao iniciar e o recarrega diariamente.
  - `SpTransController`: endpoints REST em `/api/sptrans`.

## ✅ Pré-requisitos
- **Java 17** e **Maven 3.9+** (ou Docker)
- Token da API Olho Vivo, gerado no [portal de desenvolvedores da SPTrans](https://www.sptrans.com.br/desenvolvedores/).

## ⚙️ Configuração
Defina o token na variável de ambiente `SPTRANS_API_TOKEN`. Nunca versione o token.
```bash
export SPTRANS_API_TOKEN=SEU_TOKEN_AQUI
```

| Propriedade | Padrão | Descrição |
|---|---|---|
| `sptrans.api.url` | `https://api.olhovivo.sptrans.com.br/v2.1` | URL base do Olho Vivo. |
| `sptrans.api.session-ttl` | `PT20M` | Tempo máximo de reuso do cookie de sessão. |
| `sptrans.gtfs.url` | download público da SPTrans | Origem do GTFS (`https://`, `file:` ou `classpath:`). |
| `sptrans.gtfs.enabled` | `true` | Desligue para subir sem rede (os endpoints de paradas respondem 503). |
| `sptrans.gtfs.atualizacao-cron` | `0 30 4 * * *` | Recarga diária do GTFS (horário de São Paulo). |
| `sptrans.cors.allowed-origins` | `http://localhost:5500,http://127.0.0.1:5500` | Origens liberadas para front-ends hospedados fora da API. |
| `spring.cloud.openfeign.client.config.default.connect-timeout` / `read-timeout` | `3000` / `10000` ms | Timeouts das chamadas à SPTrans. |

## ▶️ Como executar
```bash
mvn test              # testes
mvn spring-boot:run   # sobe em http://localhost:8080
```
Com Docker:
```bash
docker build -t radarsptrans .
docker run -e SPTRANS_API_TOKEN=SEU_TOKEN -p 8080:8080 radarsptrans
```
O mapa fica em `http://localhost:8080/` e o health check em `/actuator/health`.

## 🗺️ Mapa
Busque uma linha pelo painel e escolha o sentido: os ônibus aparecem no mapa, atualizados a cada 15 segundos (a atualização pausa com a aba em segundo plano). Com a localização do navegador, ou clicando no mapa para definir sua posição, o mapa mostra:
- as paradas a até 400 m, com as linhas que passam em cada uma;
- a parada da linha escolhida mais próxima de você, com o próximo ônibus e o intervalo programado.

## 🌐 Endpoints
A autenticação na SPTrans é feita pela própria API. Horários são no fuso de São Paulo.

### `GET /api/sptrans/linhas?termosBusca=`
Linhas que correspondem ao termo (número, letreiro como `8000-10` ou parte do nome), uma por sentido.
```json
[{"codigo": 1273, "letreiro": "8000-10", "sentido": 1, "origem": "TERM. LAPA", "destino": "PÇA. RAMOS DE AZEVEDO", "circular": false}]
```
> `sentido` segue a SPTrans: 1 vai do terminal secundário ao principal, 2 o contrário. A documentação oficial diz o inverso, mas as paradas e o GTFS confirmam este comportamento; `origem`/`destino` já vêm resolvidos.

### `GET /api/sptrans/posicao?codigoLinha=`
Posição dos ônibus de uma linha (`codigo` de `/linhas`).
```json
{"horaReferencia": "20:02", "veiculos": [{"prefixo": "81612", "acessivel": true, "atualizadoEm": "2026-09-26T23:01:40Z", "latitude": -23.60, "longitude": -46.79}]}
```

### `GET /api/sptrans/paradas/proximas?latitude=&longitude=[&raio=500][&limite=20]`
Paradas a até `raio` metros (máx. 2000), da mais próxima para a mais distante, com as linhas que passam em cada uma. Vem do GTFS: a API Olho Vivo não tem busca de paradas por coordenada.

### `GET /api/sptrans/paradas/tempo-espera?termosBusca=&latitude=&longitude=[&codigoLinha=]`
Para cada linha encontrada pelo termo (ou só a de `codigoLinha`), acha a parada **dela** mais próxima do ponto e calcula a espera. Termos amplos retornam as 4 linhas com parada mais perto.
```json
[{
  "linha": {"codigo": 504, "letreiro": "7545-10", "sentido": 1, "origem": "JD. JOÃO XXIII", "destino": "PÇA. RAMOS DE AZEVEDO", "circular": false},
  "parada": {"codigo": 260015039, "nome": "Paulista B/C", "endereco": "R. Da Consolação, 2483 ...", "latitude": -23.5559, "longitude": -46.6631},
  "distanciaMetros": 89,
  "intervaloProgramadoMinutos": 15.0,
  "esperaMediaMinutos": 7.5,
  "chegadas": [{"prefixo": "81443", "minutos": 9, "horario": "20:13", "acessivel": true}],
  "proximaChegadaMinutos": 9,
  "intervaloObservadoMinutos": null,
  "fonteChegadas": "PREVISAO_SPTRANS",
  "horaReferencia": "20:04"
}]
```
Como cada campo é calculado:
- **`intervaloProgramadoMinutos`**: intervalo entre ônibus previsto no GTFS para o dia e horário atuais (`null` se a linha não opera agora). O GTFS da SPTrans tem uma única grade por linha e sentido; não diferencia fins de semana nos intervalos, só os dias em que a linha roda.
- **`esperaMediaMinutos`**: metade do intervalo programado, que é a espera média de quem chega à parada sem olhar o horário, supondo ônibus regulares.
- **`chegadas`** / **`fonteChegadas`**:
  - `PREVISAO_SPTRANS`: previsão oficial da SPTrans, que considera o trânsito. Só existe para parte das paradas de cada linha (na 7545-10, 11 de 39).
  - `POSICAO_VEICULOS`: nas demais paradas, cada ônibus é associado à parada do itinerário mais próxima dele e a chegada é o tempo de percurso **programado** até a parada alvo. É uma aproximação: comparada com a previsão oficial em 44 casos reais, errou 3 min na mediana, mas 15 min ou mais em 1 de cada 5.
  - `SEM_DADOS`: nenhum ônibus a caminho.
- **`intervaloObservadoMinutos`**: intervalo médio entre as chegadas em tempo real (precisa de pelo menos 2).

### `GET /api/sptrans/buscar?termosBusca=&indice=` (descontinuado)
Retorna a posição da linha na posição `indice` da busca. O índice depende da ordem da SPTrans; use `/linhas` + `/posicao`. A resposta traz o cabeçalho `Deprecation: true`.

### Erros
Erros retornam `{"code": "...", "message": "..."}`:

| HTTP | `code` | Quando |
|------|--------|--------|
| 400 | `PARAMETRO_INVALIDO` | Parâmetro ausente, não numérico ou fora do intervalo. |
| 400 | `LINHA_INDICE_INVALIDO` | `indice` maior que a quantidade de linhas encontradas. |
| 502 | `AUTENTICACAO_FALHOU` | Token ausente ou recusado pela SPTrans. |
| 502 | `COOKIE_SESSAO_NAO_ENCONTRADO` | SPTrans não devolveu cookie de sessão. |
| 502 | `SPTRANS_INDISPONIVEL` | Timeout, falha de rede ou erro HTTP da SPTrans. |
| 503 | `DADOS_PROGRAMADOS_INDISPONIVEIS` | GTFS ainda carregando (alguns segundos após subir) ou download falhou. |

## 📦 Dependências principais
- Spring Boot 3.3 (Web, Validation, Cache, Actuator, DevTools, Test)
- Spring Cloud OpenFeign 2023.0.3
- Caffeine (cache) e Apache Commons CSV (leitura do GTFS)

## 🧪 Testes
`mvn test` roda a suíte sem acessar a SPTrans (o GTFS é desligado nos testes de contexto). O CI (`.github/workflows/ci.yml`) roda os testes e o build da imagem Docker a cada PR.

## 📄 Licença
Este repositório é disponibilizado sem licença explícita. Consulte o autor antes de uso comercial.
