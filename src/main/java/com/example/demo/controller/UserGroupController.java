package com.example.demo.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.UserGroup;
import com.example.demo.repository.UserGroupRepository;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "UserGroup", description = "API của UserGroup")
@RestController
@RequestMapping("/usergroups")
public class UserGroupController {
  private final UserGroupRepository userGroupRepository;

  public UserGroupController(UserGroupRepository userGroupRepository) {
    this.userGroupRepository = userGroupRepository;
  }

  @GetMapping
  public List<UserGroup> getAllUserGroup() {
    return userGroupRepository.findAll();
  }
}
