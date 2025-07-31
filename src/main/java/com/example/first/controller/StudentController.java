package com.example.first.controller;

import com.example.first.model.Student;
import com.example.first.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.Map;

@RestController
@RequestMapping("/students")
public class StudentController {

    @Autowired
    private StudentRepository studentRepo;

    @Value("${spring.data.mongodb.stage:default}")
    private String stage;

    // Custom health endpoint
    @GetMapping("/health")
    public ResponseEntity<Object> health() {
        System.out.println("Health check endpoint called. Stage: `${stage}`");
        return ResponseEntity.ok(Map.of("status", "Live", "stage", stage));
    }

    // Create
    @PostMapping
    public ResponseEntity<Object> createStudent(@RequestBody Student student) {
        System.out.println("Creating student: " + student);
        return ResponseEntity.ok(studentRepo.save(student));
    }

    // Read All
    @GetMapping
    public ResponseEntity<Object> getAllStudents() {
        System.out.println("Fetching all students from the database");
        return ResponseEntity.ok(studentRepo.findAll());
    }

    // Read by ID
    @GetMapping("/{id}")
    public ResponseEntity<Object> getStudentById(@PathVariable String id) {
        System.out.println("Fetching student with ID: " + id);
        Optional<Student> student = studentRepo.findById(id);
        if (student.isPresent()) {
            return ResponseEntity.ok(student.get());
        } else {
            return ResponseEntity.status(404).body("Student not found");
        }
    }

    // Update
  @PutMapping("/{id}")
public ResponseEntity<Student> updateStudent(@PathVariable String id, @RequestBody Student studentDetails) {
    System.out.println("Updating student with ID: " + id + " with details: " + studentDetails);
    Student student = studentRepo.findById(id).orElseThrow(() -> new RuntimeException("Student not found"));
    student.setName(studentDetails.getName());
    student.setEmail(studentDetails.getEmail());
    Student updatedStudent = studentRepo.save(student);
    return ResponseEntity.ok(updatedStudent);
}


    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteStudent(@PathVariable String id) {
        System.out.println("Deleting student with ID: " + id);
        studentRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
