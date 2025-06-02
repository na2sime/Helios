
# Helios ORM 🌟

Helios is a lightweight Object-Relational Mapper (ORM) designed for PostgreSQL, leveraging the power of **HikariCP** for connection pooling. Its goal is to make interacting with databases simple and efficient. 🚀

## Features ✨

- 🔌 **Easy Integration**: Seamlessly connect to your PostgreSQL database.
- 📜 **Annotations-based Mapping**: Use annotations like `@Table`, `@Column`, and `@OneToMany` for database entity definitions.
- 🧑‍🤝‍🧑 **Relations Support**: Handles `@OneToMany`, `@ManyToOne`, and `@ManyToMany` relations effortlessly.
- ⚡ **Lazy & Eager Loading**: Optimize your queries with flexible data fetching strategies.

---

## Getting Started 🛠️

### Installation

Add the Helios dependency to your project:

If you're using Maven, include the following in your `pom.xml`:

```xml
<dependency>
    <groupId>fr.nassime.helios</groupId>
    <artifactId>helios</artifactId>
    <version>1.0.0</version>
</dependency>
```

If you're using Gradle, add this to your `build.gradle`:

```groovy
dependencies {
    implementation 'fr.nassime.helios:helios:1.0.0'
}
```

---

## Usage 📖

### Database Configuration

Set up your database connection using `DataSourceConfig`:

```java
DataSourceConfig config = DataSourceConfig.builder()
    .jdbcUrl("jdbc:postgresql://localhost:5432/your_database")
    .username("postgres")
    .password("password")
    .schema("public")
    .build();
```

---

### Entity Definitions 🏗️

Define your database entities using annotations.

#### Department

```java
@Data
@Table(name = "departments")
public static class Department {
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @OneToMany(targetEntity = Employee.class, mappedBy = "department", orphanRemoval = true)
    private List<Employee> employees;
}
```

#### Employee

```java
@Data
@Table(name = "employees")
public static class Employee {
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String email;

    @Column(name = "hire_date")
    private LocalDateTime hireDate;

    @ManyToOne(joinColumn = "department_id")
    private Department department;
}
```

#### Project

```java
@Data
@Table(name = "projects")
public static class Project {
    @Id
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column
    private LocalDateTime deadline;

    @ManyToMany(
        targetEntity = Employee.class,
        joinTable = "project_members",
        joinColumn = "project_id",
        inverseJoinColumn = "employee_id"
    )
    private List<Employee> members;
}
```

---

### Save and Fetch Data 🗂️

#### Save Data

```java
try (HeliosORM orm = HeliosORM.create(config)) {
    Department department = new Department();
    department.setName("Research & Development");
    Department savedDepartment = orm.save(department);
    System.out.println("Saved Department ID: " + savedDepartment.getId());
}
```

#### Fetch Data with Relations

```java
Optional<Department> foundDepartment = orm.findById(Department.class, savedDepartment.getId());
foundDepartment.ifPresent(d -> {
    orm.loadRelation(d, "employees");
    System.out.println("Employees: " + d.getEmployees().size());
});
```

---

## Contributing 🤝

We welcome contributions! Feel free to open issues or submit pull requests. Let's make Helios even better! 🌟

---

## License 📜

Helios is released under the [MIT License](LICENSE).

---

## Authors ✍️

- [Nassime](https://github.com/na2sime)

---
