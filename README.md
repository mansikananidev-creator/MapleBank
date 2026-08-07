<div align="center">

# Maple Bank

**A secure, production-structured Canadian banking system with a REST API backend**

Built with Spring Boot 3 · Java 21 · JWT · MySQL · RabbitMQ

[![Backend CI](https://github.com/mansikananidev-creator/MapleBank/actions/workflows/backend-ci.yml/badge.svg)](https://github.com/mansikananidev-creator/MapleBank/actions/workflows/backend-ci.yml)
[![Java](https://img.shields.io/badge/Java_21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](#)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot_3-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](#)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](#)
[![MySQL](https://img.shields.io/badge/MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](#)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](#)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](#)

</div>


## About

Maple Bank is a banking system that simulates core banking functionality for the Canadian market: account
management, fund transfers, loan processing, recurring payments, and large-transaction compliance reporting,
with a focus on clean architecture and security best practices.

**Highlights:**
- Stateless JWT authentication with Spring Security, including brute-force login lockout
- Self-service forgot/reset password flow, emailed via Gmail SMTP - sending is
  decoupled from the request itself through a RabbitMQ queue, with automatic retry
  and a dead-letter queue for messages that keep failing
- Editable profile with KYC-style personal info (phone, date of birth, address) and a
  logged-in change-password flow, with server-side validation (18+, Canadian postal code format)
- Role-based authorization via `@PreAuthorize`, not scattered manual role checks
- Full loan lifecycle: apply, approve/reject, repay
- Interac-style e-Transfers (send money by email instead of account number)
- Recurring/scheduled payments via a background job
- FINTRAC-inspired large-transaction ($10,000+ CAD) compliance flagging and admin review
- Paginated transaction history and a monthly income/spending summary per account
- Global exception handling with structured error responses
- Clean layered architecture: Controller, Service, Repository
- Unit + integration test suite (JUnit 5, Mockito, H2) and CI on every push
- Interactive API docs via Swagger/OpenAPI


## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Security | Spring Security + JWT (jjwt 0.12) |
| Persistence | Spring Data JPA + MySQL (H2 for tests) |
| Messaging | RabbitMQ (Spring AMQP) |
| API Docs | springdoc-openapi (Swagger UI) |
| Testing | JUnit 5, Mockito, Spring Test, H2 |
| CI | GitHub Actions |
| Utilities | Lombok |
| Build | Maven |


## Getting Started

Want a public link instead of running it locally? See [DEPLOYMENT.md](DEPLOYMENT.md)
for a free deployment walkthrough (Aiven + CloudAMQP + Render + Vercel).

### Option A: Docker (fastest)

Requires only Docker and Docker Compose - no local Java, Maven, MySQL, or RabbitMQ
needed.

```bash
git clone https://github.com/mansikananidev-creator/MapleBank.git
cd MapleBank
docker compose up
```

That's it. The API comes up at `http://localhost:8081` with MySQL and RabbitMQ
alongside it, all using local-dev-only default credentials so it works out of the
box. RabbitMQ's management UI is at `http://localhost:15672` (login `guest`/`guest`)
if you want to watch messages move through the queue. To customize anything (a real
`JWT_SECRET`, Gmail credentials for password-reset emails, etc.), copy `.env.example`
to `.env` and fill in what you need before running `docker compose up` - see the
comments in that file for details.

### Option B: Run locally

#### Prerequisites

- Java 21+
- Maven 3.9+ (or use the bundled `./mvnw` wrapper)
- MySQL 8+
- RabbitMQ (or just run `docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672
  rabbitmq:3-management` and skip installing it directly)

#### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/mansikananidev-creator/MapleBank.git
   cd MapleBank
   ```

2. **Configure the database**

   Create a MySQL database named `unibank`. Connection settings live in
   `src/main/resources/application.properties` and default to `root` / a local
   fallback password — override them with environment variables instead of editing
   the file directly:

   ```bash
   export DB_PASSWORD=your_mysql_password
   export JWT_SECRET=$(openssl rand -base64 32)   # never commit a real secret
   ```

   **Optional — enable forgot/reset password emails.** These are sent via Gmail SMTP.
   Without this set, the forgot-password endpoint still works exactly the same from the
   caller's point of view (same response either way, by design — see Security notes
   below), it just won't actually deliver an email; the failure is logged server-side
   instead.

   1. Turn on 2-Step Verification on the Gmail account you want to send from:
      Google Account → Security → 2-Step Verification.
   2. Generate an **App Password**: Google Account → Security → 2-Step Verification →
      App passwords. Choose "Other", name it anything (e.g. "Maple Bank"), and copy the
      16-character code it gives you.
   3. Set these environment variables (use the app password, not your real Gmail password):
      ```bash
      export MAIL_USERNAME=your.email@gmail.com
      export MAIL_PASSWORD=your16charapppassword
      ```

3. **Run the application**
   ```bash
   ./mvnw spring-boot:run
   ```

   The API will be available at `http://localhost:8081` (see `server.port` in
   `application.properties`). Interactive API docs are at
   `http://localhost:8081/swagger-ui.html`.

### Running tests

```bash
./mvnw test
```

Tests run against an in-memory H2 database (`src/test/resources/application.properties`),
so no MySQL instance is required to run the suite - and RabbitMQ's auto-configuration is
excluded in tests too (with a mocked `RabbitTemplate` where it's needed), so no broker
needs to be running either. The same command runs in CI on every push and pull request
via GitHub Actions (`.github/workflows/backend-ci.yml`).


## Security notes

- Passwords are hashed with BCrypt; the app never stores or logs plaintext passwords.
- JWTs are stateless and signed with HS256. The signing key is read from the `JWT_SECRET`
  environment variable, falling back to a local-dev-only value so the app still runs out
  of the box — set `JWT_SECRET` explicitly for anything beyond local development.
- Accounts lock for 15 minutes after 5 consecutive failed login attempts.
- Password reset tokens are single-use, expire after 30 minutes, and `forgot-password`
  always responds the same way whether or not the email is registered, so the endpoint
  can't be used to enumerate valid accounts. A successful reset also clears any lockout.
- `forgot-password` is also rate-limited per account (one reset email per 60 seconds),
  so it can't be used to flood a real user's inbox. This is enforced by a small
  in-memory `RateLimiter` - fine for a single-instance deployment like this one, though
  a multi-instance deployment would need a shared store (e.g. Redis) instead.
- Admin-only operations (loan approval/rejection, compliance review) are enforced with
  Spring Security's `@PreAuthorize`, and denials return a structured JSON error via a
  custom `AccessDeniedHandler` rather than a default error page.
- Every money-moving operation verifies the caller owns the account/loan involved before
  acting on it.


## REST API Reference

All endpoints except `/api/auth/**` and the Swagger docs require a `Bearer` JWT token in
the `Authorization` header. Full interactive documentation (request/response schemas,
try-it-out) is available at `/swagger-ui.html` once the app is running.

### Auth
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login and receive a JWT (locks after 5 failed attempts) |
| `POST` | `/api/auth/forgot-password` | Request a password reset email (same response whether or not the email exists) |
| `POST` | `/api/auth/reset-password` | Reset a password using the token from that email (expires in 30 minutes, single-use) |

### Accounts
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/accounts` | Open a new account |
| `GET` | `/api/accounts` | Get all accounts for the authenticated user |
| `GET` | `/api/accounts/{id}` | Get a specific account by ID |
| `DELETE` | `/api/accounts/{id}` | Close an account |

### Transactions
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/transactions/deposit` | Deposit funds into an account |
| `POST` | `/api/transactions/withdraw` | Withdraw funds from an account |
| `POST` | `/api/transactions/transfer` | Transfer funds to another account by account number |
| `POST` | `/api/transactions/transfer/email` | Interac-style transfer to another user by email |
| `GET` | `/api/transactions/account/{accountId}` | Get paginated transaction history |
| `GET` | `/api/transactions/recent` | Get the 10 most recent transactions across all accounts |
| `GET` | `/api/transactions/summary` | Monthly income vs. expense totals (last 6 months) |

### Loans
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/loans` | Apply for a loan |
| `PUT` | `/api/loans/{id}/approve` | Approve a loan (admin only) |
| `PUT` | `/api/loans/{id}/reject` | Reject a loan (admin only) |
| `POST` | `/api/loans/{id}/repay` | Make a loan repayment (borrower only) |
| `GET` | `/api/loans` | Get all loans for the authenticated user |
| `GET` | `/api/loans/pending` | Get all pending loans (admin only) |

### Recurring payments
| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/recurring-payments` | Schedule a weekly/monthly recurring payment |
| `GET` | `/api/recurring-payments` | Get the authenticated user's recurring payments |
| `DELETE` | `/api/recurring-payments/{id}` | Cancel a recurring payment |

### Profile
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/profile` | Get the authenticated user's profile |
| `PUT` | `/api/profile` | Update personal info (name, phone, DOB, address) |
| `PUT` | `/api/profile/change-password` | Change password (requires current password) |

### Compliance (admin only)
| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/compliance/flags` | Get all flagged large transactions |
| `PUT` | `/api/compliance/flags/{id}/review` | Mark a flagged transaction as reviewed |


## Project Structure

```
src/main/java/com/unibank/bankingSystem/
├── config/                      # Cross-cutting configuration
│   └── RabbitMQConfig            # Queue/exchange/dead-letter setup, JSON message converter
├── controller/                  # REST controllers
│   ├── AccountController
│   ├── AuthController
│   ├── ComplianceController
│   ├── LoanController
│   ├── ProfileController
│   ├── RecurringPaymentController
│   └── TransactionController
├── dto/                         # Request and response objects
│   ├── AccountRequest / AccountResponse
│   ├── AuthResponse
│   ├── ComplianceFlagResponse
│   ├── EmailTransferRequest
│   ├── ErrorResponse
│   ├── ForgotPasswordRequest / ResetPasswordRequest / ChangePasswordRequest
│   ├── LoanRequest / LoanResponse / LoanRepaymentRequest
│   ├── LoginRequest / RegisterRequest
│   ├── MonthlySummaryResponse
│   ├── RecurringPaymentRequest / RecurringPaymentResponse
│   ├── TransactionRequest / TransactionResponse / TransferRequest
│   └── UpdateProfileRequest / UserProfileResponse
├── exception/                   # Custom exceptions and global handler
│   ├── AccountLockedException
│   ├── BadRequestException
│   ├── DuplicateResourceException
│   ├── GlobalExceptionHandler
│   ├── InsufficientFundsException
│   ├── ResourceNotFoundException
│   └── UnauthorizedException
├── model/                       # JPA entities and enums
│   ├── Account, ComplianceFlag, Loan, PasswordResetToken, RecurringPayment, Transaction, User
│   └── AccountStatus, AccountType, Frequency, LoanStatus, Role, TransType (enums)
├── repository/                  # Spring Data repositories
│   ├── AccountRepository
│   ├── ComplianceFlagRepository
│   ├── LoanRepository
│   ├── PasswordResetTokenRepository
│   ├── RecurringPaymentRepository
│   ├── TransactionRepository
│   └── UserRepository
├── messaging/                   # RabbitMQ message contracts and listeners
│   ├── PasswordResetEmailMessage  # Payload published to the password-reset queue
│   └── PasswordResetEmailListener # Consumes it and calls EmailService, with retry
├── security/                    # JWT and Spring Security config
│   ├── JwtAuthFilter
│   ├── JwtService
│   ├── SecurityConfig
│   └── UserDetailsServiceImpl
└── service/                     # Business logic
    ├── AccountService
    ├── AuthService
    ├── ComplianceService
    ├── EmailService
    ├── LoanService
    ├── RateLimiter
    ├── RecurringPaymentService
    ├── TransactionService
    └── UserService

src/test/java/com/unibank/bankingSystem/
├── AuthFlowIntegrationTest         # Full-stack register/login/JWT/lockout test (MockMvc + H2)
├── BankingSystemApplicationTests   # Spring context load test
├── messaging/
│   └── PasswordResetEmailListenerTest  # Listener calls EmailService with the right args
└── service/
    ├── AuthServiceTest             # Login lockout, forgot/reset password logic
    ├── ComplianceServiceTest       # Large-transaction flagging threshold
    ├── EmailServiceTest            # Sends correctly, propagates failures for retry
    ├── LoanServiceTest             # Loan ownership checks, repayment/payoff logic
    ├── RateLimiterTest             # Per-key cooldown behavior
    ├── TransactionServiceTest      # Transfer validation and balance movement
    └── UserServiceTest             # Profile validation (age, postal code) and change-password

Dockerfile               # Multi-stage build: Maven build -> slim JRE runtime image
docker-compose.yml       # Backend + MySQL, wired together for one-command startup
.env.example             # Template for overriding Docker Compose defaults
```


## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
