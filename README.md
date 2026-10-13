# email-send

API REST (Spring Boot) para envio **assíncrono** de e-mails, usando Kafka e MySQL.

## Como funciona

<p align="center">
 <img src="https://github.com/user-attachments/assets/124fd493-017f-4166-bf88-66ed53002a0f" alt="Image" width="800" />

</p>


Se o Kafka estiver fora na hora do POST, o e-mail é marcado como `ERRO` em vez de ficar `PENDENTE` para sempre.

## Rodando

1. `cp .env.example .env` e preencha `API_KEY`, `DB_PASSWORD`, `MAIL_USERNAME` e `MAIL_PASSWORD`
   (no Gmail, use uma *Senha de app*).
2. `docker compose up -d` (MySQL + Kafka).
3. Exporte as variáveis do `.env` (ou configure na IDE) e rode `./mvnw spring-boot:run`.

A aplicação **não sobe** sem `API_KEY`, `DB_PASSWORD`, `MAIL_USERNAME` e `MAIL_PASSWORD`: não há valores padrão para segredos.

## Uso

```bash
curl -i -X POST http://localhost:8080/email \
  -H "Content-Type: application/json" \
  -H "X-API-Key: $API_KEY" \
  -d '{"assunto":"Olá","destinatario":"alguem@exemplo.com","mensagem":"Teste"}'

curl -H "X-API-Key: $API_KEY" http://localhost:8080/email/1
```

Swagger UI: http://localhost:8080/swagger-ui.html (use **Authorize** para informar a API key).

## Testes

```bash
./mvnw test
```

Os testes não precisam de MySQL, Kafka nem SMTP (H2 em memória e mocks).

## Limitações conhecidas

- Entrega "pelo menos uma vez": se o SMTP enviar e o banco falhar logo em seguida, a reentrega pode duplicar o e-mail.
- Sem rate limit; a proteção é só a API key.
- `ddl-auto=update` é para desenvolvimento; em produção use Flyway/Liquibase.
