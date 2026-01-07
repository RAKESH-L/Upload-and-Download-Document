package com.document.model;

import java.util.Arrays;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;

//FIX: Added validation imports
import jakarta.validation.constraints.NotBlank; //FIX: Added for input validation
import jakarta.validation.constraints.Size; //FIX: Added for input validation

@Entity
public class Document {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Document name must not be blank") //FIX: Input validation for 'name'
    @Size(min = 1, max = 255, message = "Document name must be between 1 and 255 characters") //FIX: Input validation for 'name' length
    private String name;

    @NotBlank(message = "File path must not be blank") //FIX: Input validation for 'filePath' (proxy for content)
    @Size(max = 1024, message = "File path must be at most 1024 characters") //FIX: Input validation for 'filePath' length
    private String filePath;
    
	public Document(Long id, String name, String filePath) {
		super();
		this.id = id;
		this.name = name;
		this.filePath = filePath;
	}
	public Document() {
		super();
		// TODO Auto-generated constructor stub
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFilePath() {
		return filePath;
	}
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	@Override
	public String toString() {
		return "Document [id=" + id + ", name=" + name + ", filePath=" + filePath + "]";
	}

//    @Lob
//    @Column(length = 10485760) // Specify the length based on your needs (e.g., 10 MB in bytes)
//    private byte[] content;
    

	
}