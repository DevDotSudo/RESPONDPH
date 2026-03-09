package com.ionres.respondph.model;

import com.ionres.respondph.admin.AdminModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AdminModel")
class AdminModelTest {

    @Nested
    @DisplayName("Constructors")
    class Constructors {

        @Test
        @DisplayName("default constructor creates empty model")
        void defaultConstructor() {
            AdminModel admin = new AdminModel();
            assertEquals(0, admin.getId());
            assertNull(admin.getUsername());
            assertNull(admin.getRole());
        }

        @Test
        @DisplayName("6-arg constructor sets fields (no role)")
        void sixArgConstructor() {
            AdminModel admin = new AdminModel("user1", "John", "M", "Doe", "2024-01-01", "pass123");
            assertEquals("user1", admin.getUsername());
            assertEquals("John", admin.getFirstname());
            assertEquals("M", admin.getMiddlename());
            assertEquals("Doe", admin.getLastname());
            assertEquals("2024-01-01", admin.getRegDate());
            assertEquals("pass123", admin.getPassword());
            assertNull(admin.getRole());
        }

        @Test
        @DisplayName("7-arg constructor sets all fields including role")
        void sevenArgConstructor() {
            AdminModel admin = new AdminModel("user1", "John", "M", "Doe",
                    "2024-01-01", "pass123", "Admin");
            assertEquals("Admin", admin.getRole());
            assertEquals("user1", admin.getUsername());
        }
    }

    @Nested
    @DisplayName("Getters and Setters")
    class GettersSetters {

        @Test
        @DisplayName("id getter/setter")
        void idGetterSetter() {
            AdminModel admin = new AdminModel();
            admin.setId(42);
            assertEquals(42, admin.getId());
        }

        @Test
        @DisplayName("all field setters work correctly")
        void allSetters() {
            AdminModel admin = new AdminModel();
            admin.setUsername("admin2");
            admin.setFirstname("Jane");
            admin.setMiddlename("A");
            admin.setLastname("Smith");
            admin.setRegDate("2024-06-15");
            admin.setPassword("secret");
            admin.setRole("Secretary");

            assertEquals("admin2", admin.getUsername());
            assertEquals("Jane", admin.getFirstname());
            assertEquals("A", admin.getMiddlename());
            assertEquals("Smith", admin.getLastname());
            assertEquals("2024-06-15", admin.getRegDate());
            assertEquals("secret", admin.getPassword());
            assertEquals("Secretary", admin.getRole());
        }
    }

    @Nested
    @DisplayName("toString()")
    class ToStringTests {

        @Test
        @DisplayName("includes id, username, and role")
        void format() {
            AdminModel admin = new AdminModel("user1", "John", "M", "Doe",
                    "2024-01-01", "pass", "Admin");
            admin.setId(10);
            String str = admin.toString();
            assertTrue(str.contains("10"));
            assertTrue(str.contains("user1"));
            assertTrue(str.contains("Admin"));
        }
    }
}

