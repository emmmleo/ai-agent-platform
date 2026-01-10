package com.aiagent.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserProfileUpdateRequest {

    @Size(max = 2097152, message = "头像数据过大")
    private String avatarUrl;

    @Size(max = 16, message = "性别字段长度需≤16")
    private String gender;

    private java.time.LocalDate birthday;

    @Size(max = 128, message = "学校长度需≤128")
    private String school;

    @Size(max = 32, message = "电话长度需≤32")
    private String phone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度需≤128")
    private String email;

    @Size(max = 512, message = "个性签名长度需≤512")
    private String bio;

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public java.time.LocalDate getBirthday() {
        return birthday;
    }

    public void setBirthday(java.time.LocalDate birthday) {
        this.birthday = birthday;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
