# marketplace-auction-java21

Projeto Spring Boot 3 / Java 21 com marketplace, leilão, autenticação JWT e upload local de imagens já preparado para futura troca por S3.

## Autenticação

### Registrar usuário
`POST /api/auth/register`

```json
{
  "nome": "Maria",
  "email": "maria@email.com",
  "password": "123456"
}
```

### Login
`POST /api/auth/login`

```json
{
  "email": "maria@email.com",
  "password": "123456"
}
```

Resposta:

```json
{
  "token": "jwt...",
  "userId": 1,
  "nome": "Maria",
  "email": "maria@email.com",
  "role": "USER"
}
```

Use o token no header:

```http
Authorization: Bearer SEU_TOKEN
```

## Upload de imagem do produto

Agora o cadastro de produto usa `multipart/form-data`.

`POST /api/products`

Campos:
- `nome`
- `descricao`
- `categoria`
- `condicao`
- `image` (arquivo opcional)
- `imageUrl` (URL externa opcional)

Se enviar `image` e `imageUrl`, o arquivo enviado terá prioridade.

As imagens locais ficam disponíveis em:
- `GET /uploads/{arquivo}`

## Banco de dados

O projeto continua pronto para rodar com H2 local.
No arquivo `src/main/resources/application.yml` foi deixado um bloco comentado com a configuração do PostgreSQL para ativação futura.

## Preparação para S3

No `application.yml` existe a seção `app.storage` com:
- `upload-dir`
- `public-base-url`
- `s3-enabled`
- `s3-bucket`
- `s3-region`

Hoje o projeto salva localmente, mas a estrutura já foi separada para facilitar a troca por um serviço de armazenamento S3.

## Endpoints públicos

- `GET /`
- `GET /api/products`
- `GET /api/listings`
- `GET /api/listings/{id}`
- `GET /api/listings/{id}/bids`
- `GET /uploads/**`
- Swagger e H2 console

## Endpoints protegidos

- `GET /api/users/me`
- `POST /api/products`
- `POST /api/listings`
- `POST /api/listings/{id}/bids`
- `POST /api/listings/{id}/buy-now`

## Regras novas

- o usuário autenticado vira o vendedor ao criar produto
- só o dono do produto pode abrir anúncio dele
- o usuário autenticado vira o comprador no buy-now
- o usuário autenticado vira o autor do lance
- senha é armazenada com BCrypt
