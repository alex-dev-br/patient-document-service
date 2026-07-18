# Postman — Patient Document Service

Esta pasta contém os artefatos usados para validar manualmente o **Patient Document Service** em ambiente local e por meio do Kong.

## Arquivos

```text
postman/
├── README.md
├── Meu Histórico Saúde - Local ou Kong.postman_environment.json
├── Meu Histórico Saúde - Patient Document Service.postman_collection.json
└── files/
    └── exame-laboratorial-ficticio.png
```

O arquivo de imagem é fictício e deve ser usado somente para testes.

## Pré-requisitos

- Docker Desktop em execução.
- Stack local iniciada pelo `docker compose`.
- Postman Desktop.
- Arquivo `.env` local preenchido com os secrets do Keycloak.

Na raiz do projeto:

```powershell
docker compose up -d
docker compose ps
```

Confirme que a API está acessível em `http://localhost:8080` e o Keycloak em `http://localhost:8081`.

## Importação no Postman

Importe os dois arquivos JSON desta pasta:

1. `Meu Histórico Saúde - Patient Document Service.postman_collection.json`
2. `Meu Histórico Saúde - Local ou Kong.postman_environment.json`

Depois selecione o environment **Meu Histórico Saúde - Local ou Kong**.

Preencha apenas os secrets no environment:

| Variável | Origem local |
|---|---|
| `devClientSecret` | valor de `PATIENT_DOCUMENT_DEV_CLIENT_SECRET` no `.env` |
| `aiClientSecret` | valor de `AI_PROCESSING_CLIENT_SECRET` no `.env` |

Não salve esses valores no Git.

## Variáveis

### Environment

| Variável | Valor local padrão | Finalidade |
|---|---|---|
| `baseUrl` | `http://localhost:8080` | URL pública da API |
| `keycloakBaseUrl` | `http://localhost:8081` | URL usada para obter tokens |
| `realm` | `meu-historico-saude` | Realm do Keycloak |
| `devClientId` | `patient-document-dev-client` | Cliente técnico de desenvolvimento |
| `devClientSecret` | vazio | Secret do cliente de desenvolvimento |
| `aiClientId` | `ai-processing-service` | Cliente técnico do processador de IA |
| `aiClientSecret` | vazio | Secret do processador de IA |

### Collection

Estas variáveis começam vazias e são preenchidas automaticamente durante os testes:

| Variável | Preenchida por |
|---|---|
| `accessToken` | `Obter token - cliente de desenvolvimento` |
| `aiAccessToken` | `Obter token - serviço de IA` |
| `patientId` | `Cadastrar paciente` |
| `documentId` | `Enviar documento` |

As variáveis operacionais ficam na collection para evitar conflito com valores antigos salvos no environment.

## Fluxo recomendado

Execute as requisições nesta ordem:

1. `00 - Autenticação > Obter token - cliente de desenvolvimento`
2. `02 - Pacientes > Cadastrar paciente`
3. `02 - Pacientes > Consultar paciente por ID`
4. `03 - Documentos do paciente > Enviar documento`
5. `03 - Documentos do paciente > Listar documentos paginados`
6. `03 - Documentos do paciente > Consultar timeline`
7. `04 - Documentos individuais > Consultar documento por ID`
8. `04 - Documentos individuais > Baixar arquivo original - cliente de desenvolvimento`
9. `00 - Autenticação > Obter token - serviço de IA`
10. `04 - Documentos individuais > Atualizar resultado da IA manualmente`
11. Execute novamente as consultas de documento, resultados processados e timeline.
12. Execute a pasta `05 - Matriz de autorização`.

### Seleção do arquivo

Na requisição `Enviar documento`, abra:

```text
Body > form-data > file
```

Selecione manualmente:

```text
postman/files/exame-laboratorial-ficticio.png
```

A referência local do arquivo não é exportada de forma portável pelo Postman. Por isso, cada pessoa que importar a collection deve selecionar o arquivo na própria máquina.

## Escopos esperados

### Cliente de desenvolvimento

O token de `patient-document-dev-client` deve conter:

```text
patients:read
patients:write
documents:read
documents:write
documents:file:read
documents:ai-result:write
```

### Serviço de IA

O token de `ai-processing-service` deve conter somente:

```text
documents:file:read
documents:ai-result:write
```

Isso permite ao processador baixar o arquivo original e publicar o resultado do processamento, sem acessar os endpoints de pacientes.

## Organização da collection

A collection possui 17 requisições:

| Pasta | Cobertura |
|---|---|
| `00 - Autenticação` | Tokens OAuth 2.0 pelo fluxo `client_credentials` |
| `01 - Documentação pública` | OpenAPI JSON e Swagger UI sem autenticação |
| `02 - Pacientes` | Cadastro, listagem e consulta por ID |
| `03 - Documentos do paciente` | Upload, paginação e timeline |
| `04 - Documentos individuais` | Consulta, resultados, download e atualização da IA |
| `05 - Matriz de autorização` | Cenários esperados de `401`, `403` e `200` |

## Matriz de autorização

| Cenário | Resultado esperado |
|---|---:|
| Listar pacientes sem token | `401 Unauthorized` |
| Listar pacientes com token da IA | `403 Forbidden` |
| Baixar arquivo com token da IA | `200 OK` |
| Atualizar resultado com token da IA | `200 OK` |
| Documentação OpenAPI e Swagger sem token | `200 OK` |

## Teste por meio do Kong

Para direcionar as requisições da API ao Kong, altere `baseUrl` no environment para a URL e rota publicadas pelo gateway.

Exemplo conceitual:

```text
http://localhost:<porta-do-kong>/<rota-da-api>
```

Mantenha `keycloakBaseUrl` separado. Ele só deve ser alterado quando o endpoint de autenticação também estiver publicado em outro endereço.

## Atualização manual do resultado da IA

A requisição `Atualizar resultado da IA manualmente` simula a resposta que será produzida pelo processador de IA.

Ela usa `aiAccessToken` e exige:

```text
documents:ai-result:write
```

Esse teste é útil enquanto o fluxo Kafka e o processador ainda não estão sendo executados de ponta a ponta. No fluxo integrado, a atualização deve ocorrer automaticamente.

## Solução de problemas

### `401 Unauthorized` com token expirado

Os tokens locais expiram rapidamente. Execute novamente a requisição de obtenção do token correspondente.

### `403 Forbidden` com `insufficient_scope`

Confirme se o token possui o escopo exigido pelo endpoint. Para a atualização da IA, o token precisa de `documents:ai-result:write`.

### `missing file source` ou “arquivo enviado está vazio”

Selecione novamente o arquivo em `Body > form-data > file`. Não reutilize uma referência `postman-cloud://` exportada por outra pessoa.

### `patientId` ou `documentId` não definido

Execute as etapas anteriores do fluxo:

- `Cadastrar paciente` gera `patientId`.
- `Enviar documento` gera `documentId`.

### Erro ao salvar o arquivo no Nextcloud

Confirme que o serviço está ativo e que o hostname Docker `nextcloud` está configurado como domínio confiável pelo `docker-compose.yml`.

## Segurança antes de exportar

Antes de versionar uma nova exportação:

1. Limpe `accessToken`, `aiAccessToken`, `patientId` e `documentId` nas variáveis da collection.
2. Limpe `devClientSecret` e `aiClientSecret` no environment.
3. Confirme que não há tokens JWT no JSON.
4. Confirme que não existe referência `postman-cloud://` no upload.
5. Valide novamente o JSON e o fluxo principal.

Os arquivos versionados devem conter somente URLs locais, IDs públicos dos clientes e variáveis vazias para secrets e dados temporários.
