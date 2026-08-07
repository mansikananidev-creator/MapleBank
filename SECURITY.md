# Security Policy

## Supported Versions

UniBank is a personal portfolio project. Only the latest commit on `main` is actively maintained.

| Version | Supported |
|---------|-----------|
| latest (main) | Yes |
| older commits | No |

## Reporting a Vulnerability

Please do not report security vulnerabilities through public GitHub issues.

If you discover a security vulnerability, report it privately through GitHub's built-in advisory tool:

1. Go to the [Security tab](https://github.com/mansikananidev-creator/MapleBank/security) of this repository
2. Click **"Report a vulnerability"** to open a private security advisory
3. Include as much of the following as you can:
    - A description of the vulnerability and its potential impact
    - Steps to reproduce or proof-of-concept
    - The affected component (e.g. JWT handling, authentication, a specific endpoint)
    - Any suggested fix if you have one

## What to Expect

After you submit a report:

- You will receive an acknowledgement within 48 hours
- I will investigate and keep you updated on progress
- If the vulnerability is confirmed, a fix will be prioritised and you will be credited if you wish

## Security Notes

This project uses:

- JWT (jjwt 0.12.5) for stateless authentication
- Spring Security for endpoint protection and password encoding
- Spring Validation for input sanitization
- Custom exception handling to avoid leaking internal details in error responses

> This project is intended for educational and portfolio purposes. Do not deploy it in a production environment without a thorough independent security review.