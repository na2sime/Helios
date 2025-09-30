package fr.nassime.helios.mongo.entities;

import fr.nassime.helios.api.annotations.*;
import fr.nassime.helios.api.annotations.enums.PersistenceType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Test entity with index annotations to validate automatic index creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Persistable(name = "users_with_indexes", type = PersistenceType.DOCUMENT)
@Index(fields = {"email", "active"}, directions = {1, -1}, name = "email_active_compound")
public class UserWithIndexes {
    
    @Id
    private String id;
    
    @Index(unique = true, sparse = true)
    @Field(name = "username")
    private String username;
    
    @Index(name = "email_idx")
    @Field(name = "email") 
    private String email;
    
    @Field(name = "age")
    private Integer age;
    
    @Field(name = "active")
    private Boolean active;
    
    @Field(name = "created_at")
    private Long createdAt;
    
    public UserWithIndexes(String username, String email, Integer age, Boolean active) {
        this.username = username;
        this.email = email;
        this.age = age;
        this.active = active;
        this.createdAt = System.currentTimeMillis();
    }
}