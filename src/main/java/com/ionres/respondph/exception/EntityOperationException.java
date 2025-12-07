package com.ionres.respondph.exception;

public class EntityOperationException extends DomainException {

    public enum Operation {
        CREATE, UPDATE, DELETE, FIND  // 👈 Add FIND
    }

    private final String entity;
    private final Operation operation;

    public EntityOperationException(String entity, Operation operation) {
        super(generateMessage(entity, operation));
        this.entity = entity;
        this.operation = operation;
    }

    public EntityOperationException(String entity, Operation operation, Throwable cause) {
        super(generateMessage(entity, operation), cause);
        this.entity = entity;
        this.operation = operation;
    }

    private static String generateMessage(String entity, Operation op) {
        if (op == Operation.FIND) {
            return "Entity not found: " + entity;
        }
        return "Failed to " + op.name().toLowerCase() + " " + entity + ".";
    }

    public String getEntity() { return entity; }
    public Operation getOperation() { return operation; }
}