# Occupify Core Documentation

All API specifications, error codes, architecture topologies, and service ports have been centralized in the `occupify-ai-engineer` workspace:

- **Centralized System Architecture & Ports:** [occupify-ai-engineer/docs/README.md](../../occupify-ai-engineer/docs/README.md)
- **Core API Specification:** [occupify-ai-engineer/docs/core/api_specification.md](../../occupify-ai-engineer/docs/core/api_specification.md)
- **Core Error Codes:** [occupify-ai-engineer/docs/core/error_code.md](../../occupify-ai-engineer/docs/core/error_code.md)

## Testing

### Unit and Slice Tests
Run fast unit and slice tests with H2 in-memory database:
```bash
./mvnw clean test
```

### Integration Tests with Testcontainers (Podman / Docker)
Integration tests run against real PostgreSQL 16 and RabbitMQ 3.13 containers using Testcontainers and `maven-failsafe-plugin`:
```bash
systemctl --user enable --now podman.socket
export DOCKER_HOST=unix:///run/user/$(id -u)/podman/podman.sock
export TESTCONTAINERS_RYUK_DISABLED=true
./mvnw clean verify
# or using Makefile:
make test-it
```

