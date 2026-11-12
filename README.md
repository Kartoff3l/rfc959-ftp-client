# Java CLI FTP Client (RFC 959)

A lightweight command-line FTP client written from scratch in pure Java (`java.net.*`, `java.io.*`) with zero third-party dependencies.

## Features
- **Control & Data Separation:** Persistent TCP control channel (port 21) and temporary passive-mode (`PASV`) data sockets for directory listings and binary file transfers.
- **RFC 959 Protocol Handling:** Parses single-line and multi-line server responses (`230-` / `250-`) and dynamically computes `PASV` data ports (`p1 * 256 + p2`).
- **Supported Commands:** `connect`, `login` (anonymous & custom), `pwd`, `cd`, `ls`, `get`, `put`, `mkd`, `rmd`, `dele`, `quit`.
- **Error & Resource Safety:** Guards against blocking I/O freezes on `5xx` server errors and guarantees stream/socket cleanup via `try-catch-finally`.

## How to Run
```bash
javac FtpClient.java
java FtpClient
