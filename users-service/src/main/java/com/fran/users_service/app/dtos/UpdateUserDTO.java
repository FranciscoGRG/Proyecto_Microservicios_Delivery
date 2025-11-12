package com.fran.users_service.app.dtos;

public class UpdateUserDTO {

    private String name;
    private String currentPassword;
    private String newPassword;

    

    public UpdateUserDTO(String name, String currentPassword, String newPassword) {
        this.name = name;
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    public UpdateUserDTO() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

}
