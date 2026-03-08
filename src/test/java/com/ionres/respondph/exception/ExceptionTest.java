package com.ionres.respondph.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Exception Classes")
class ExceptionTest {

    // ─── DomainException ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("DomainException")
    class DomainExceptionTests {

        @Test
        @DisplayName("stores message correctly")
        void messageConstructor() {
            DomainException ex = new DomainException("test error");
            assertEquals("test error", ex.getMessage());
        }

        @Test
        @DisplayName("stores message and cause correctly")
        void messageAndCauseConstructor() {
            RuntimeException cause = new RuntimeException("root");
            DomainException ex = new DomainException("wrapper", cause);
            assertEquals("wrapper", ex.getMessage());
            assertSame(cause, ex.getCause());
        }

        @Test
        @DisplayName("stores cause correctly")
        void causeConstructor() {
            RuntimeException cause = new RuntimeException("root cause");
            DomainException ex = new DomainException(cause);
            assertSame(cause, ex.getCause());
        }

        @Test
        @DisplayName("is a RuntimeException")
        void isRuntimeException() {
            assertInstanceOf(RuntimeException.class, new DomainException("test"));
        }
    }

    // ─── ValidationException ──────────────────────────────────────────────────

    @Nested
    @DisplayName("ValidationException")
    class ValidationExceptionTests {

        @Test
        @DisplayName("stores validation message")
        void messageStored() {
            ValidationException ex = new ValidationException("field required");
            assertEquals("field required", ex.getMessage());
        }

        @Test
        @DisplayName("is a DomainException")
        void isDomainException() {
            assertInstanceOf(DomainException.class, new ValidationException("test"));
        }
    }

    // ─── DuplicateEntityException ─────────────────────────────────────────────

    @Nested
    @DisplayName("DuplicateEntityException")
    class DuplicateEntityExceptionTests {

        @Test
        @DisplayName("message includes entity name and 'already exists'")
        void messageFormat() {
            DuplicateEntityException ex = new DuplicateEntityException("Admin");
            assertTrue(ex.getMessage().contains("Admin"));
            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("is a DomainException")
        void isDomainException() {
            assertInstanceOf(DomainException.class, new DuplicateEntityException("User"));
        }
    }

    // ─── EntityOperationException ─────────────────────────────────────────────

    @Nested
    @DisplayName("EntityOperationException")
    class EntityOperationExceptionTests {

        @Test
        @DisplayName("CREATE generates 'Failed to create' message")
        void createMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Beneficiary", EntityOperationException.Operation.CREATE);
            assertEquals("Failed to create Beneficiary.", ex.getMessage());
            assertEquals("Beneficiary", ex.getEntity());
            assertEquals(EntityOperationException.Operation.CREATE, ex.getOperation());
        }

        @Test
        @DisplayName("UPDATE generates 'Failed to update' message")
        void updateMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Disaster", EntityOperationException.Operation.UPDATE);
            assertTrue(ex.getMessage().contains("update"));
        }

        @Test
        @DisplayName("DELETE generates 'Failed to delete' message")
        void deleteMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Aid", EntityOperationException.Operation.DELETE);
            assertTrue(ex.getMessage().contains("delete"));
        }

        @Test
        @DisplayName("FIND generates 'Entity not found' message")
        void findMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "EvacSite #5", EntityOperationException.Operation.FIND);
            assertEquals("Entity not found: EvacSite #5", ex.getMessage());
        }

        @Test
        @DisplayName("stores cause when provided")
        void withCause() {
            Exception cause = new RuntimeException("db error");
            EntityOperationException ex = new EntityOperationException(
                    "Admin", EntityOperationException.Operation.CREATE, cause);
            assertSame(cause, ex.getCause());
        }
    }

    // ─── ExceptionFactory ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("ExceptionFactory")
    class ExceptionFactoryTests {

        @Test
        @DisplayName("passwordMismatch creates ValidationException")
        void passwordMismatch() {
            ValidationException ex = ExceptionFactory.passwordMismatch();
            assertInstanceOf(ValidationException.class, ex);
            assertTrue(ex.getMessage().contains("Passwords do not match"));
        }

        @Test
        @DisplayName("missingField creates ValidationException with field name")
        void missingField() {
            ValidationException ex = ExceptionFactory.missingField("Username");
            assertTrue(ex.getMessage().contains("Username"));
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("duplicate creates DuplicateEntityException")
        void duplicate() {
            DuplicateEntityException ex = ExceptionFactory.duplicate("Admin", "john123");
            assertTrue(ex.getMessage().contains("john123"));
            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("failedToCreate creates EntityOperationException")
        void failedToCreate() {
            EntityOperationException ex = ExceptionFactory.failedToCreate("Beneficiary");
            assertEquals(EntityOperationException.Operation.CREATE, ex.getOperation());
        }

        @Test
        @DisplayName("failedToUpdate creates EntityOperationException")
        void failedToUpdate() {
            EntityOperationException ex = ExceptionFactory.failedToUpdate("Disaster");
            assertEquals(EntityOperationException.Operation.UPDATE, ex.getOperation());
        }

        @Test
        @DisplayName("failedToDelete creates EntityOperationException")
        void failedToDelete() {
            EntityOperationException ex = ExceptionFactory.failedToDelete("Aid");
            assertEquals(EntityOperationException.Operation.DELETE, ex.getOperation());
        }

        @Test
        @DisplayName("entityNotFound creates EntityOperationException with FIND")
        void entityNotFound() {
            EntityOperationException ex = ExceptionFactory.entityNotFound("Beneficiary #99");
            assertEquals(EntityOperationException.Operation.FIND, ex.getOperation());
            assertTrue(ex.getMessage().contains("Beneficiary #99"));
        }
    }
}

