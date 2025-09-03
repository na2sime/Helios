package fr.nassime.helios.mongo.entities;

import fr.nassime.helios.api.annotations.Document;
import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;
import fr.nassime.helios.api.annotations.OneToMany;
import fr.nassime.helios.api.annotations.enums.FetchType;

import java.util.List;

/**
 * Test entity representing a Department with OneToMany relationship to Employee.
 */
@Document(collection = "departments")
public class Department {
    
    @Id
    private String id;
    
    @Field(name = "name")
    private String name;
    
    @Field(name = "code")
    private String code;
    
    @Field(name = "description")
    private String description;
    
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees;
    
    // No-arg constructor
    public Department() {
    }
    
    // Constructor with fields
    public Department(String name, String code, String description) {
        this.name = name;
        this.code = code;
        this.description = description;
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
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public List<Employee> getEmployees() {
        return employees;
    }
    
    public void setEmployees(List<Employee> employees) {
        this.employees = employees;
    }
    
    @Override
    public String toString() {
        return "Department{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", description='" + description + '\'' +
                ", employees=" + (employees != null ? employees.size() : 0) + " employees" +
                '}';
    }
}