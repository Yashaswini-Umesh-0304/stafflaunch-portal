package net.javaguides.springboot.tutorial.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

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
    
    // RELATIONAL MAPPING
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hardware_bundle_id")
    private HardwareBundle assetBundle;

    private boolean assetAcknowledged = false;
    
    // RELATIONAL MAPPING
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_read_articles", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "article_slug")
    private List<String> readArticles = new ArrayList<>();

    private String role; 
    private boolean enabled = false; 

    public Employee() {}

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
    
    // Maintains backward compatibility for your HTML
    public String getAssets() { return assetBundle != null ? assetBundle.getDescription() : null; }
    public void setAssetBundle(HardwareBundle assetBundle) { this.assetBundle = assetBundle; }
    
    public boolean isAssetAcknowledged() { return assetAcknowledged; }
    public void setAssetAcknowledged(boolean assetAcknowledged) { this.assetAcknowledged = assetAcknowledged; }
    public List<String> getReadArticles() { return readArticles; }
    public void setReadArticles(List<String> readArticles) { this.readArticles = readArticles; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}