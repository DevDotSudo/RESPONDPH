package com.ionres.respondph.exception;

public class ExceptionFactory {

    public static ValidationException passwordMismatch() {
        return new ValidationException("Passwords do not match.");
    }

    public static ValidationException missingField(String fieldName) {
        return new ValidationException(fieldName + " is required.");
    }

    public static DuplicateEntityException duplicate(String entityName, String identifier) {
        return new DuplicateEntityException(entityName + " with value '" + identifier + "' already exists.");
    }

    public static EntityOperationException failedToCreate(String entity) {
        return new EntityOperationException(entity, EntityOperationException.Operation.CREATE);
    }

    public static EntityOperationException failedToUpdate(String entity) {
        return new EntityOperationException(entity, EntityOperationException.Operation.UPDATE);
    }

    public static EntityOperationException failedToDelete(String entity) {
        return new EntityOperationException(entity, EntityOperationException.Operation.DELETE);
    }

    public static EntityOperationException entityNotFound(String entityDetails) {
        return new EntityOperationException(entityDetails, EntityOperationException.Operation.FIND);
    }
}
