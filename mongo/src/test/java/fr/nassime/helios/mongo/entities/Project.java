package fr.nassime.helios.mongo.entities;

import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;
import fr.nassime.helios.api.annotations.ManyToMany;
import fr.nassime.helios.api.annotations.Persistable;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.mongo.annotations.Reference;

import java.util.List;

/**
 * Test entity representing a Project with ManyToMany relationship to Employee.
 */
@Persistable(name = "projects", type = PersistenceType.DOCUMENT)
public class Project {
    
    @Id
    private String id;
    
    @Field(name = "name")
    private String name;
    
    @Field(name = "description")
    private String description;
    
    @Field(name = "budget")
    private Double budget;
    
    @Field(name = "status")
    private String status;
    
    // Foreign keys array for Employees (ManyToMany)
    @Reference
    @Field(name = "employee_ids")
    private List<String> employeeIds;
    
    // ManyToMany relationship (loaded via employeeIds)
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "projects")
    private List<Employee> employees;
    
    // No-arg constructor
    public Project() {
    }
    
    // Constructor with fields
    public Project(String name, String description, Double budget, String status) {
        this.name = name;
        this.description = description;
        this.budget = budget;
        this.status = status;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Double getBudget() {
        return budget;
    }
    
    public void setBudget(Double budget) {
        this.budget = budget;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<String> getEmployeeIds() {
        return employeeIds;
    }
    
    public void setEmployeeIds(List<String> employeeIds) {
        this.employeeIds = employeeIds;
    }
    
    public List<Employee> getEmployees() {
        return employees;
    }
    
    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
    
    @Override
    public String toString() {
        return "Project{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", budget=" + budget +
                ", status='" + status + '\'' +
                ", employees=" + (employees != null ? employees.size() : 0) + " employees" +
                '}';
    }
}