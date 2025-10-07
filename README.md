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
1. Renomeie o arquivo `src/main/resources/application.properties` ou crie um `application-local.properties` conforme sua estratégia.
2. Defina o token do Olho Vivo substituindo o valor de `sptrans.api.token` ou exportando a variável de ambiente:
   ```bash
   export SPRING_APPLICATION_JSON='{"sptrans":{"api":{"token":"SEU_TOKEN_AQUI"}}}'
   ```
3. Ajuste as origens permitidas configurando a propriedade `sptrans.cors.allowed-origins`. Por padrão ela já inclui `http://localhost:5500` e `http://127.0.0.1:5500`, mas você pode sobrescrevê-la no `application.properties` ou via variável de ambiente:
   ```bash
   export SPRING_APPLICATION_JSON='{"sptrans":{"cors":{"allowed-origins":"https://minhaapp.com,https://admin.minhaapp.com"}}}'
   ```
   > Use uma lista separada por vírgulas para definir todas as origens necessárias em ambientes de produção ou desenvolvimento.

> ⚠️ O token presente no repositório é apenas ilustrativo. Gere seu próprio token no [portal da SPTrans](http://www.sptrans.com.br/desenvolvedores/).

## ▶️ Como executar
```bash
# Executar os testes
mvn test

# Subir a aplicação
mvn spring-boot:run
```
A API ficará disponível em `http://localhost:8080`.

## 🌐 Endpoints
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

> Utilize o endpoint `/buscar` para obter o código (`cl`) de uma linha antes de consultar sua posição.

## 🗺️ Front-end de visualização (opcional)
O diretório `front/` contém um protótipo simples com Leaflet para exibir os ônibus em mapa.
1. Inicie a API localmente.
2. Abra `front/mapa.html` com uma extensão de servidor estático (ex.: Live Server do VSCode) para evitar bloqueios CORS.
3. Atualize a URL da API no arquivo `front/mapa.js` caso necessário.

## 📦 Dependências principais
- Spring Boot 3.3 (Web, DevTools, Test)
- Spring Cloud OpenFeign 2023.0.3
- Jackson Databind 2.15.2
- Lombok (opcional para getters/setters)

## 🧪 Testes
O projeto utiliza `spring-boot-starter-test`. Execute `mvn test` para garantir a integridade após alterações.

## 📄 Licença
Este repositório é disponibilizado sem licença explícita. Consulte o autor antes de uso comercial.
