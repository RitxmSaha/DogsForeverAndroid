package com.example.dogsforeverandroid;

import java.util.HashMap;
import java.util.Map;

public class User {

    public String username;
    public String email;
    public int shelterID = 0;
    public static int UserID = 0;

    public User() {
        // Default constructor required for calls to DataSnapshot.getValue(User.class)
        UserID++;
    }

    public User(String username, String email) {
        this.username = username;
        this.email = email;
        UserID++;
    }
    //nonzero shelterID are real shelter ID's.
    public void setShelterID(int ID) {
        shelterID = ID;
    }

    public Map<String,Object> getTaskMap() {
        Map<String,Object> taskMap = new HashMap<>();
        taskMap.put("username", username);
        taskMap.put("email", email);
        taskMap.put("Unique User ID", UserID);
        taskMap.put("Shelter ID", shelterID);
        return taskMap;
    }


}
