package com.example.facedemo.repository;

import com.example.facedemo.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {

    /** 按姓名查找人员（用于注册时判断是否已存在） */
    Optional<Person> findByName(String name);
}
