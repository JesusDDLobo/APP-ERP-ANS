package com.example.app_ans.tasks.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/** Simplified user model for assigned technicians */
public class AssignedUser implements Serializable {
    private int id;
    private String name;
    private String email;
    
    @SerializedName("employee_id")
    private String employeeId;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getEmployeeId() { return employeeId; }
}
