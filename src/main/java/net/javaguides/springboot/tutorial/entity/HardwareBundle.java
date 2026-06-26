package net.javaguides.springboot.tutorial.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "hardware_bundles")
public class HardwareBundle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String bundleName;
    private String description;

    public HardwareBundle() {}

    public HardwareBundle(String bundleName, String description) {
        this.bundleName = bundleName;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getBundleName() { return bundleName; }
    public String getDescription() { return description; }
}