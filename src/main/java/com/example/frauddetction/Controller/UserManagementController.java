package com.example.frauddetction.Controller;

import com.example.frauddetction.service.UserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class UserManagementController {

    @Autowired
    private UserRoleService userRoleService;

    @PostMapping("/add-user")
    public String addUser(@RequestParam String username,
                          @RequestParam String password,
                          @RequestParam String role) {
        userRoleService.createUser(username, password, role);
        return "redirect:/admin-dashboard";
    }

    @PostMapping("/delete-user")
    public String deleteUser(@RequestParam String username) {
        userRoleService.deleteUser(username);
        return "redirect:/admin-dashboard";
    }
}