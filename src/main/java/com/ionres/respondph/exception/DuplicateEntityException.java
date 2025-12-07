package com.ionres.respondph.exception;

public class DuplicateEntityException extends DomainException {
    public DuplicateEntityException(String entityName) {
        super(entityName + " already exists.");
    }
}
