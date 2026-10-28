# Agent RPC Contract Fixtures

This directory contains recorded contract fixtures for the Occupify Agent RPC interface:
- `search.json`: Response for fixed search request on synthetic catalogue
- `get.json`: Response for single project detail retrieval (without employer data)
- `get-sparse.json`: Response for sparse project (demonstrating null policy on optional fields and empty collections)
- `get-404.json`: Error response when project is not found (`PROJ_001`)
- `get-400.json`: Error response on bad input (`INVALID_ARGUMENT`)
- `get-500.json`: Error response on unexpected internal error (`INTERNAL_SERVER_ERROR`)

These fixtures are recorded and verified by `AgentRpcContractIT`:
```bash
# Record fixtures
./mvnw verify -Dit.test=AgentRpcContractIT -Dcontracts.record=true

# Verify against fixtures
./mvnw verify -Dit.test=AgentRpcContractIT
```
