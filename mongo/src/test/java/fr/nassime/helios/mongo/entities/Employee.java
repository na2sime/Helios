package fr.nassime.helios.mongo.entities;

import fr.nassime.helios.api.annotations.Field;
import fr.nassime.helios.api.annotations.Id;
import fr.nassime.helios.api.annotations.ManyToOne;
import fr.nassime.helios.api.annotations.ManyToMany;
import fr.nassime.helios.api.annotations.Persistable;
import fr.nassime.helios.api.annotations.enums.FetchType;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import fr.nassime.helios.mongo.annotations.Reference;

import java.util.List;

/**
 * Test entity representing an Employee with ManyToOne relationship to Department
 * and ManyToMany relationship to Project.
 */
@Persistable(name = "employees", type = PersistenceType.DOCUMENT)
public class Employee {
    
    @Id
    private String id;
    
    @Field(name = "firstname")
    private String firstname;
    
    @Field(name = "lastname")
    private String lastname;
    
    @Field(name = "email")
    private String email;
    
    @Field(name = "salary")
    private Double salary;
    
    // Foreign key to Department
    @Reference
    @Field(name = "department_id")
    private String departmentId;
    
    // ManyToOne relationship (loaded via departmentId)
    @ManyToOne(fetch = FetchType.EAGER)
    private Department department;
    
    // Foreign keys array for Projects (ManyToMany)
    @Reference
    @Field(name = "project_ids")
    private List<String> projectIds;
    
    // ManyToMany relationship (loaded via projectIds)
    @ManyToMany(fetch = FetchType.LAZY)
    private List<Project> projects;
    
    // No-arg constructor
    public Employee() {
    }
    
    // Constructor with fields
    public Employee(String firstname, String lastname, String email, Double salary) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.salary = salary;
    }
    
    // Constructor with department
    public Employee(String firstname, String lastname, String email, Double salary, String departmentId) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.salary = salary;
        this.departmentId = departmentId;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getFirstname() {
        return firstname;
    }
    
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }
    
    public String getLastname() {
        return lastname;
    }
    
    public void setLastname(String lastname) {
        this.lastname = lastname;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Double getSalary() {
        return salary;
    }
    
    public void setSalary(Double salary) {
        this.salary = salary;
    }
    
    public String getDepartmentId() {
        return departmentId;
    }
    
    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
    
    public Department getDepartment() {
        return department;
    }
    
    public void setDepartment(Department department) {
        this.department = department;
    }
    
    public List<String> getProjectIds() {
        return projectIds;
    }
    
    public void setProjectIds(List<String> projectIds) {
        this.projectIds = projectIds;
    }
    
    public List<Project> getProjects() {
        return projects;
    }
    
    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }
    
    @Override
    public String toString() {
        return "Employee{" +
                "id='" + id + '\'' +
                ", firstname='" + firstname + '\'' +
                ", lastname='" + lastname + '\'' +
                ", email='" + email + '\'' +
                ", salary=" + salary +
                ", departmentId='" + departmentId + '\'' +
                ", department=" + (department != null ? department.getName() : null) +
                ", projects=" + (projects != null ? projects.size() : 0) + " projects" +
                '}';
    }
}