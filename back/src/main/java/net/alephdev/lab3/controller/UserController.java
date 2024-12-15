package net.alephdev.lab3.controller;

import net.alephdev.lab3.annotation.AuthorizedRequired;
import net.alephdev.lab3.annotation.CurrentUser;
import net.alephdev.lab3.models.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@AuthorizedRequired
public class UserController {

    @GetMapping
    public ResponseEntity<User> getCurrentUser(@CurrentUser User user) {
        return ResponseEntity.ok(user);
    }
}