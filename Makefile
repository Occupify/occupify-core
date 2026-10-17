COMPOSE ?= podman compose

.PHONY: help up infra down

infra:
	@$(COMPOSE) --env-file core.env up -d

up:
	@./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

down:
	@$(COMPOSE) down
