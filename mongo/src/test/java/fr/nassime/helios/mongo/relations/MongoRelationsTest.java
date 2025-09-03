package fr.nassime.helios.mongo.relations;

import fr.nassime.helios.mongo.AbstractMongoTest;
import fr.nassime.helios.mongo.entities.Department;
import fr.nassime.helios.mongo.entities.Employee;
import fr.nassime.helios.mongo.entities.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for MongoDB relationships (@OneToMany, @ManyToOne, @ManyToMany).
 */
class MongoRelationsTest extends AbstractMongoTest {
    
    @BeforeEach
    void setUpTest() {
        clearDatabase();
    }
    
    @Test
    void testManyToOneRelation() {
        System.out.println("=== Testing ManyToOne Relation ===");
        
        // Create and save a department
        Department department = new Department("Engineering", "ENG", "Software Engineering Department");
        Department savedDept = session.save(department);
        
        assertNotNull(savedDept.getId());
        System.out.println("1. Created Department: " + savedDept);
        
        // Create and save an employee with department reference
        Employee employee = new Employee("John", "Doe", "john.doe@company.com", 75000.0, savedDept.getId());
        Employee savedEmployee = session.save(employee);
        
        assertNotNull(savedEmployee.getId());
        assertEquals(savedDept.getId(), savedEmployee.getDepartmentId());
        System.out.println("2. Created Employee: " + savedEmployee);
        
        // Find the employee by ID (should load EAGER department relation)
        Optional<Employee> foundEmployeeOpt = session.findById(Employee.class, savedEmployee.getId());
        assertTrue(foundEmployeeOpt.isPresent());
        
        Employee foundEmployee = foundEmployeeOpt.get();
        System.out.println("3. Found Employee with relations: " + foundEmployee);
        
        // Check that the ManyToOne relation was loaded (EAGER)
        assertNotNull(foundEmployee.getDepartment());
        assertEquals("Engineering", foundEmployee.getDepartment().getName());
        assertEquals("ENG", foundEmployee.getDepartment().getCode());
        
        System.out.println("✓ ManyToOne relation test passed!");
    }
    
    @Test
    void testOneToManyRelation() {
        System.out.println("=== Testing OneToMany Relation ===");
        
        // Create and save a department
        Department department = new Department("Marketing", "MKT", "Marketing Department");
        Department savedDept = session.save(department);
        
        // Create and save multiple employees for this department
        Employee emp1 = new Employee("Alice", "Smith", "alice.smith@company.com", 65000.0, savedDept.getId());
        Employee emp2 = new Employee("Bob", "Johnson", "bob.johnson@company.com", 70000.0, savedDept.getId());
        Employee emp3 = new Employee("Charlie", "Brown", "charlie.brown@company.com", 68000.0, savedDept.getId());
        
        session.save(emp1);
        session.save(emp2);
        session.save(emp3);
        
        System.out.println("1. Created Department and 3 Employees");
        
        // Load the OneToMany relation manually (since it's LAZY)
        session.loadRelation(savedDept, "employees");
        
        // Check that employees were loaded
        assertNotNull(savedDept.getEmployees());
        assertEquals(3, savedDept.getEmployees().size());
        
        System.out.println("2. Loaded Department with employees: " + savedDept.getEmployees().size());
        for (Employee emp : savedDept.getEmployees()) {
            System.out.println("   - " + emp.getFirstname() + " " + emp.getLastname());
        }
        
        System.out.println("✓ OneToMany relation test passed!");
    }
    
    @Test
    void testManyToManyRelation() {
        System.out.println("=== Testing ManyToMany Relation ===");
        
        // Create and save projects
        Project project1 = new Project("Website Redesign", "Redesign company website", 50000.0, "Active");
        Project project2 = new Project("Mobile App", "Develop mobile application", 80000.0, "Planning");
        
        Project savedProject1 = session.save(project1);
        Project savedProject2 = session.save(project2);
        
        System.out.println("1. Created Projects: " + savedProject1.getName() + ", " + savedProject2.getName());
        
        // Create employees
        Employee emp1 = new Employee("David", "Wilson", "david.wilson@company.com", 85000.0);
        Employee emp2 = new Employee("Emma", "Davis", "emma.davis@company.com", 90000.0);
        
        // Set up ManyToMany relationships (Employee -> Projects)
        emp1.setProjectIds(Arrays.asList(savedProject1.getId(), savedProject2.getId()));
        emp2.setProjectIds(Arrays.asList(savedProject1.getId()));
        
        Employee savedEmp1 = session.save(emp1);
        Employee savedEmp2 = session.save(emp2);
        
        // Set up reverse ManyToMany relationships (Project -> Employees)
        savedProject1.setEmployeeIds(Arrays.asList(savedEmp1.getId(), savedEmp2.getId()));
        savedProject2.setEmployeeIds(Arrays.asList(savedEmp1.getId()));
        
        session.save(savedProject1);
        session.save(savedProject2);
        
        System.out.println("2. Set up ManyToMany relationships");
        
        // Load ManyToMany relations
        session.loadRelation(savedEmp1, "projects");
        session.loadRelation(savedProject1, "employees");
        
        // Check Employee -> Projects relation
        assertNotNull(savedEmp1.getProjects());
        assertEquals(2, savedEmp1.getProjects().size());
        
        System.out.println("3. Employee " + savedEmp1.getFirstname() + " works on " + savedEmp1.getProjects().size() + " projects:");
        for (Project project : savedEmp1.getProjects()) {
            System.out.println("   - " + project.getName());
        }
        
        // Check Project -> Employees relation
        assertNotNull(savedProject1.getEmployees());
        assertEquals(2, savedProject1.getEmployees().size());
        
        System.out.println("4. Project " + savedProject1.getName() + " has " + savedProject1.getEmployees().size() + " employees:");
        for (Employee emp : savedProject1.getEmployees()) {
            System.out.println("   - " + emp.getFirstname() + " " + emp.getLastname());
        }
        
        System.out.println("✓ ManyToMany relation test passed!");
    }
    
    @Test
    void testManualRelationLoading() {
        System.out.println("=== Testing Manual Relation Loading ===");
        
        // Create test data
        Department dept = new Department("HR", "HR", "Human Resources");
        Department savedDept = session.save(dept);
        
        Employee employee = new Employee("Sarah", "Miller", "sarah.miller@company.com", 60000.0, savedDept.getId());
        Employee savedEmployee = session.save(employee);
        
        // Find employee without eager loading
        Optional<Employee> foundOpt = session.findById(Employee.class, savedEmployee.getId());
        assertTrue(foundOpt.isPresent());
        
        Employee found = foundOpt.get();
        assertNotNull(found.getDepartment()); // Should be loaded as EAGER
        
        System.out.println("1. Found employee with EAGER department: " + found.getDepartment().getName());
        
        // Test manual loading of a relation that doesn't exist
        try {
            session.loadRelation(found, "nonExistentRelation");
            fail("Should have thrown an exception for non-existent relation");
        } catch (Exception e) {
            System.out.println("2. Correctly threw exception for non-existent relation: " + e.getMessage());
        }
        
        System.out.println("✓ Manual relation loading test passed!");
    }
}