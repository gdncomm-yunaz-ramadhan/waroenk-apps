package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.User;

import java.util.List;

public interface UserCustomRepository {
  List<User> findUserLike(String query, String cursor, int size, String sortField, String sortDirection);

  Long countAll();
}
