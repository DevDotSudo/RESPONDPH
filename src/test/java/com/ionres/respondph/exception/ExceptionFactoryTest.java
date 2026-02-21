package com.ionres.respondph.exception;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the custom exception hierarchy and ExceptionFactory.
 */
@DisplayName("Exception Hierarchy & ExceptionFactory")
class ExceptionFactoryTest {

    // ═══════════════════════════════════════════════════════════════════════
    // DomainException (base class)
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DomainException — Base Exception")
    class DomainExceptionTests {

        @Test
        @DisplayName("DomainException is a RuntimeException")
        void isRuntimeException() {
            DomainException ex = new DomainException("test");
            assertInstanceOf(RuntimeException.class, ex);
        }

        @Test
        @DisplayName("Message is preserved")
        void messagePreserved() {
            DomainException ex = new DomainException("custom message");
            assertEquals("custom message", ex.getMessage());
        }

        @Test
        @DisplayName("Cause is preserved")
        void causePreserved() {
            Exception cause = new IllegalStateException("root cause");
            DomainException ex = new DomainException("wrapped", cause);
            assertEquals("wrapped", ex.getMessage());
            assertSame(cause, ex.getCause());
        }

        @Test
        @DisplayName("Cause-only constructor")
        void causeOnlyConstructor() {
            Exception cause = new NullPointerException("npe");
            DomainException ex = new DomainException(cause);
            assertSame(cause, ex.getCause());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ValidationException
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ValidationException")
    class ValidationExceptionTests {

        @Test
        @DisplayName("Extends DomainException")
        void extendsDomainException() {
            ValidationException ex = new ValidationException("field required");
            assertInstanceOf(DomainException.class, ex);
        }

        @Test
        @DisplayName("Message is preserved")
        void messagePreserved() {
            ValidationException ex = new ValidationException("Email is required.");
            assertEquals("Email is required.", ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // DuplicateEntityException
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("DuplicateEntityException")
    class DuplicateEntityExceptionTests {

        @Test
        @DisplayName("Extends DomainException")
        void extendsDomainException() {
            DuplicateEntityException ex = new DuplicateEntityException("Admin");
            assertInstanceOf(DomainException.class, ex);
        }

        @Test
        @DisplayName("Message includes 'already exists'")
        void messageFormat() {
            DuplicateEntityException ex = new DuplicateEntityException("Admin");
            assertTrue(ex.getMessage().contains("already exists"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // EntityOperationException
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EntityOperationException")
    class EntityOperationExceptionTests {

        @Test
        @DisplayName("CREATE operation message")
        void createMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Beneficiary", EntityOperationException.Operation.CREATE);
            assertTrue(ex.getMessage().contains("create"));
            assertTrue(ex.getMessage().contains("Beneficiary"));
            assertEquals("Beneficiary", ex.getEntity());
            assertEquals(EntityOperationException.Operation.CREATE, ex.getOperation());
        }

        @Test
        @DisplayName("UPDATE operation message")
        void updateMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Disaster", EntityOperationException.Operation.UPDATE);
            assertTrue(ex.getMessage().contains("update"));
            assertTrue(ex.getMessage().contains("Disaster"));
        }

        @Test
        @DisplayName("DELETE operation message")
        void deleteMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Aid", EntityOperationException.Operation.DELETE);
            assertTrue(ex.getMessage().contains("delete"));
        }

        @Test
        @DisplayName("FIND operation message says 'not found'")
        void findMessage() {
            EntityOperationException ex = new EntityOperationException(
                    "Disaster #42", EntityOperationException.Operation.FIND);
            assertTrue(ex.getMessage().contains("not found"));
            assertTrue(ex.getMessage().contains("Disaster #42"));
        }

        @Test
        @DisplayName("Constructor with cause preserves cause")
        void withCause() {
            Exception cause = new RuntimeException("db error");
            EntityOperationException ex = new EntityOperationException(
                    "Admin", EntityOperationException.Operation.CREATE, cause);
            assertSame(cause, ex.getCause());
        }

        @Test
        @DisplayName("All Operation enum values exist")
        void allOperations() {
            EntityOperationException.Operation[] ops = EntityOperationException.Operation.values();
            assertEquals(4, ops.length);
            assertNotNull(EntityOperationException.Operation.valueOf("CREATE"));
            assertNotNull(EntityOperationException.Operation.valueOf("UPDATE"));
            assertNotNull(EntityOperationException.Operation.valueOf("DELETE"));
            assertNotNull(EntityOperationException.Operation.valueOf("FIND"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // ExceptionFactory
    // ═══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ExceptionFactory — Factory Methods")
    class FactoryMethodTests {

        @Test
        @DisplayName("passwordMismatch() → ValidationException with correct message")
        void passwordMismatch() {
            ValidationException ex = ExceptionFactory.passwordMismatch();
            assertNotNull(ex);
            assertTrue(ex.getMessage().toLowerCase().contains("password"));
            assertTrue(ex.getMessage().toLowerCase().contains("match"));
        }

        @Test
        @DisplayName("missingField() → ValidationException with field name")
        void missingField() {
            ValidationException ex = ExceptionFactory.missingField("Email");
            assertNotNull(ex);
            assertTrue(ex.getMessage().contains("Email"));
            assertTrue(ex.getMessage().contains("required"));
        }

        @Test
        @DisplayName("duplicate() → DuplicateEntityException with entity and identifier")
        void duplicate() {
            DuplicateEntityException ex = ExceptionFactory.duplicate("Admin", "john_doe");
            assertNotNull(ex);
            assertTrue(ex.getMessage().contains("Admin"));
            assertTrue(ex.getMessage().contains("john_doe"));
            assertTrue(ex.getMessage().contains("already exists"));
        }

        @Test
        @DisplayName("failedToCreate() → EntityOperationException CREATE")
        void failedToCreate() {
            EntityOperationException ex = ExceptionFactory.failedToCreate("Beneficiary");
            assertNotNull(ex);
            assertEquals(EntityOperationException.Operation.CREATE, ex.getOperation());
            assertEquals("Beneficiary", ex.getEntity());
            assertTrue(ex.getMessage().toLowerCase().contains("create"));
        }

        @Test
        @DisplayName("failedToUpdate() → EntityOperationException UPDATE")
        void failedToUpdate() {
            EntityOperationException ex = ExceptionFactory.failedToUpdate("Disaster");
            assertEquals(EntityOperationException.Operation.UPDATE, ex.getOperation());
            assertEquals("Disaster", ex.getEntity());
        }

        @Test
        @DisplayName("failedToDelete() → EntityOperationException DELETE")
        void failedToDelete() {
            EntityOperationException ex = ExceptionFactory.failedToDelete("Aid");
            assertEquals(EntityOperationException.Operation.DELETE, ex.getOperation());
            assertEquals("Aid", ex.getEntity());
        }

        @Test
        @DisplayName("entityNotFound() → EntityOperationException FIND")
        void entityNotFound() {
            EntityOperationException ex = ExceptionFactory.entityNotFound("Disaster #42");
            assertEquals(EntityOperationException.Operation.FIND, ex.getOperation());
            assertTrue(ex.getMessage().contains("not found"));
            assertTrue(ex.getMessage().contains("Disaster #42"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Exception catchability
    // ═══════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("All custom exceptions are catchable as DomainException")
    void allCatchableAsDomain() {
        assertThrows(DomainException.class, () -> {
            throw ExceptionFactory.passwordMismatch();
        });
        assertThrows(DomainException.class, () -> {
            throw ExceptionFactory.duplicate("X", "Y");
        });
        assertThrows(DomainException.class, () -> {
            throw ExceptionFactory.failedToCreate("Z");
        });
    }

    @Test
    @DisplayName("All custom exceptions are catchable as RuntimeException")
    void allCatchableAsRuntime() {
        assertThrows(RuntimeException.class, () -> {
            throw ExceptionFactory.missingField("Name");
        });
        assertThrows(RuntimeException.class, () -> {
            throw ExceptionFactory.failedToDelete("Record");
        });
    }
}

