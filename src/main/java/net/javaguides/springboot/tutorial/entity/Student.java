package net.javaguides.springboot.tutorial.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @NotBlank(message = "First Name is mandatory")
    private String firstName;
    
    private String lastName;

    @Column(unique = true)
    private String username;

    @NotBlank(message = "Email is mandatory")
    @Email(message = "Invalid format")
    @Column(unique = true)
    private String email;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    private String phoneNumber;
    private String dob;
    
    private String assets; 
    private boolean assetAcknowledged = false;
    
    // NEW FIELD: Tracks which articles the user has read (comma separated)
    private String readArticles = "";

    private String role; 
    private boolean enabled = false; 

    public Student() {}

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getDob() { return dob; }
    public void setDob(String dob) { this.dob = dob; }
    public String getAssets() { return assets; }
    public void setAssets(String assets) { this.assets = assets; }
    public boolean isAssetAcknowledged() { return assetAcknowledged; }
    public void setAssetAcknowledged(boolean assetAcknowledged) { this.assetAcknowledged = assetAcknowledged; }
    public String getReadArticles() { return readArticles; }
    public void setReadArticles(String readArticles) { this.readArticles = readArticles; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}