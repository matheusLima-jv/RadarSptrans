# RadarSptrans

## 📌 Visão Geral
O **RadarSptrans** é uma API Spring Boot que consulta o serviço público Olho Vivo da SPTrans e expõe endpoints simplificados para localizar ônibus na cidade de São Paulo. O projeto segue uma abordagem hexagonal, separando claramente camadas de domínio, aplicação e infraestrutura, e inclui um front-end estático (`front/`) para visualização em mapa.

## 🧱 Arquitetura
- **Camada de domínio**: modelos (`LinhaResponse`, `PosicaoBus`, `PosicaoBusResponse`) e portas (interfaces) que definem os contratos de uso.
- **Camada de aplicação**: `SpTransApplicationService` coordena autenticação e consulta das linhas/posições.
- **Adaptadores de infraestrutura**:
  - `SpTransAuthClientAdapter` realiza a autenticação no serviço Olho Vivo e captura o cookie de sessão.
  - `SpTransClientAdapter` consome os endpoints de busca e posição através do OpenFeign.
  - `SpTransController` expõe os endpoints REST `/api/sptrans`.
- **Configuração adicional**: `WebConfig` habilita CORS para o front-end com base na propriedade `sptrans.cors.allowed-origins`.

## ✅ Pré-requisitos
- **Java 17**
- **Maven 3.9+**
- Conta na **SPTrans** para gerar o token de acesso ao Olho Vivo.

## ⚙️ Configuração
1. Defina o token do Olho Vivo na variável de ambiente `SPTRANS_API_TOKEN` (o `application.properties` lê `sptrans.api.token=${SPTRANS_API_TOKEN:}`). Nunca versione o token.
   ```bash
   export SPTRANS_API_TOKEN=SEU_TOKEN_AQUI
   ```
2. Ajuste as origens permitidas configurando a propriedade `sptrans.cors.allowed-origins`. Por padrão ela já inclui `http://localhost:5500` e `http://127.0.0.1:5500`, mas você pode sobrescrevê-la no `application.properties` ou via variável de ambiente:
   ```bash
   export SPTRANS_CORS_ALLOWED_ORIGINS=https://minhaapp.com,https://admin.minhaapp.com
   ```
   > Use uma lista separada por vírgulas para definir todas as origens necessárias em ambientes de produção ou desenvolvimento.

Outras propriedades:

| Propriedade | Padrão | Descrição |
|---|---|---|
| `sptrans.api.url` | `https://api.olhovivo.sptrans.com.br/v2.1` | URL base do Olho Vivo. |
| `sptrans.api.session-ttl` | `PT20M` | Tempo máximo de reuso do cookie de sessão. Um 401 da SPTrans força novo login antes disso. |
| `spring.cloud.openfeign.client.config.default.connect-timeout` / `read-timeout` | `3000` / `10000` ms | Timeouts das chamadas à SPTrans. |

> Gere seu próprio token no [portal da SPTrans](http://www.sptrans.com.br/desenvolvedores/).

## ▶️ Como executar
```bash
# Executar os testes
mvn test

# Subir a aplicação
mvn spring-boot:run
```
A API ficará disponível em `http://localhost:8080`.

## 🌐 Endpoints
A autenticação na SPTrans é feita pela própria API: o cookie de sessão fica em cache e é renovado automaticamente quando expira.

### `GET /api/sptrans/linhas`
Lista as linhas que correspondem ao termo (número ou parte do nome), em ambos os sentidos.

| Parâmetro       | Tipo   | Descrição                                        |
|-----------------|--------|--------------------------------------------------|
| `termosBusca`   | query  | Termo de busca (ex.: `8000`, `Lapa`).            |

Cada item traz `cl` (código usado em `/posicao`), `lt`/`tl` (letreiro, ex.: `8000`-`10`), `sl` (sentido: 1 = `tp` → `ts`, 2 = `ts` → `tp`), `tp`/`ts` (terminais) e `lc` (linha circular).

### `GET /api/sptrans/buscar`
Busca linhas por termo textual e retorna a posição do ônibus correspondente ao índice informado (baseado na lista retornada pela SPTrans).

| Parâmetro       | Tipo   | Descrição                                                 |
|-----------------|--------|-----------------------------------------------------------|
| `termosBusca`   | query  | Termo utilizado na busca da linha (ex.: `8000`).          |
| `indice`        | query  | Posição da linha desejada na lista retornada (1-based).   |

**Resposta (`PosicaoBusResponse`)**
- `hr`: hora da última atualização.
- `vs`: lista de veículos, cada um com:
  - `p`: prefixo do veículo;
  - `a`: indica acessibilidade (booleano);
  - `ta`: timestamp da última atualização;
  - `py` / `px`: latitude e longitude;
  - `sv`, `is`: campos adicionais conforme resposta oficial da SPTrans.

### `GET /api/sptrans/posicao`
Retorna a posição de todos os ônibus de uma linha específica.

| Parâmetro       | Tipo   | Descrição                                   |
|-----------------|--------|---------------------------------------------|
| `codigoLinha`   | query  | Código numérico da linha (campo `cl`).      |

> Utilize o endpoint `/linhas` para obter o código (`cl`) de uma linha antes de consultar sua posição.

### Erros
Erros retornam `{"code": "...", "message": "..."}`:

| HTTP | `code` | Quando |
|------|--------|--------|
| 400 | `PARAMETRO_INVALIDO` | Parâmetro ausente, não numérico ou fora do intervalo. |
| 400 | `LINHA_INDICE_INVALIDO` | `indice` maior que a quantidade de linhas encontradas. |
| 502 | `AUTENTICACAO_FALHOU` | Token ausente ou recusado pela SPTrans. |
| 502 | `COOKIE_SESSAO_NAO_ENCONTRADO` | SPTrans não devolveu cookie de sessão. |
| 502 | `SPTRANS_INDISPONIVEL` | Timeout, falha de rede ou erro HTTP da SPTrans. |

## 🗺️ Front-end de visualização (opcional)
O diretório `front/` contém um mapa com Leaflet: busque uma linha pelo painel, escolha o sentido e os ônibus aparecem no mapa, atualizados a cada 15 segundos.
1. Inicie a API localmente.
2. Sirva `front/` na porta 5500 (ex.: Live Server do VSCode ou `cd front && python3 -m http.server 5500`) e abra `http://localhost:5500/mapa.html`.
3. Se a API não estiver em `http://localhost:8080`, defina `window.RADAR_API_URL` antes de carregar `mapa.js`.

## 📦 Dependências principais
- Spring Boot 3.3 (Web, Validation, DevTools, Test)
- Spring Cloud OpenFeign 2023.0.3
- Lombok (opcional para getters/setters)

## 🧪 Testes
O projeto utiliza `spring-boot-starter-test`. Execute `mvn test` para garantir a integridade após alterações.

## 📄 Licença
Este repositório é disponibilizado sem licença explícita. Consulte o autor antes de uso comercial.
