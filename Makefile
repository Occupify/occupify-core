# Auto-detect container runtime (podman or docker)
COMPOSE ?= $(shell \
	if command -v podman >/dev/null 2>&1; then \
		if podman compose version >/dev/null 2>&1; then \
			echo "podman compose"; \
		elif command -v podman-compose >/dev/null 2>&1; then \
			echo "podman-compose"; \
		else \
			echo "podman compose"; \
		fi; \
	elif command -v docker >/dev/null 2>&1; then \
		if docker compose version >/dev/null 2>&1; then \
			echo "docker compose"; \
		elif command -v docker-compose >/dev/null 2>&1; then \
			echo "docker-compose"; \
		else \
			echo "docker compose"; \
		fi; \
	else \
		echo "docker compose"; \
	fi)

PROFILES ?= dev

.PHONY: help up infra down test-it

help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  infra    Start background services (PostgreSQL, RabbitMQ) with $(COMPOSE)"
	@echo "  up       Run the Spring Boot application (profiles: $(PROFILES))"
	@echo "  down     Stop and remove containers and volumes"
	@echo "  test-it  Run all tests and integration tests with Testcontainers"
	@echo ""
	@echo "Detected compose tool: $(COMPOSE)"

infra:
	@$(COMPOSE) --env-file core.env up -d

up: infra
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=$(PROFILES)

down:
	@$(COMPOSE) down -v

test-it:
	./mvnw clean verify
