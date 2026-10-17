# Occupify Core Error Codes Specification

This document details all business and system error codes thrown by `occupify-core`.
Errors are never returned manually from controllers; they are thrown from services, argument resolvers, or validation layers and handled by `@RestControllerAdvice` (`GlobalExceptionHandler`), returning a standardized `ErrorResponse` envelope:

```json
{
  "statusCode": 404,
  "message": "Project not found with ID: a0000000-0000-0000-0000-000000000001",
  "errorCode": "PROJ_001"
}
```

## Error Codes Reference Table

| Error Code | HTTP Status | Exception Class | Description / Business Meaning | Triggering Scenario |
| :--- | :--- | :--- | :--- | :--- |
| **`PROJ_001`** | `404 NOT_FOUND` | `ProjectException` | **Project Not Found** | Thrown when retrieving, updating, or closing a project ID that does not exist in the database. |
| **`PROJ_002`** | `403 FORBIDDEN` | `ProjectException` | **Project Ownership Forbidden** | Thrown when an authenticated user attempts to mutate or close a project without necessary ownership or permissions. |
| **`PROJ_003`** | `400 BAD_REQUEST` | `ProjectException` | **Project Already Closed** | Thrown when attempting to close an already-closed project or performing an illegal state transition. |
| **`PROJ_004`** | `404 NOT_FOUND` | `ProjectException` | **Employer Record Not Found** | Thrown when the employer user referenced by `X-User-Id` does not exist in the `users` table and dev mock auto-provisioning is disabled. |
| **`SYS_001`** | `500 INTERNAL_SERVER_ERROR` | `SystemException` / `Exception` | **Internal System Error** | Unhandled server error or unexpected infrastructure failure. |
| **`SYS_002`** | `400 BAD_REQUEST` | `MethodArgumentNotValidException` | **Request Validation Error** | Thrown when DTO bean validation fails (e.g. blank `title`, negative `startPrice`, null `period`). |
| **`SYS_003`** | `404 NOT_FOUND` | `SystemException` | **Resource Not Found** | Generic resource lookup failure. |
| **`SYS_006`** | `400 BAD_REQUEST` | `SystemException` | **Invalid Parameter Format** | Thrown when `X-User-Id` header is present but is not a valid UUID string. |
| **`SYS_008`** | `401 UNAUTHORIZED` | `SystemException` | **Missing / Invalid Authentication** | Thrown when a mutation endpoint requiring `@Authenticated` is accessed without `X-User-Id` header (and dev mock is disabled). |
| **`SYS_009`** | `403 FORBIDDEN` | `SystemException` | **Forbidden Role Access** | Thrown when access is denied due to role mismatch. |
