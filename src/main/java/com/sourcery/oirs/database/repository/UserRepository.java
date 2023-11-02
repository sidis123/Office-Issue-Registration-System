package com.sourcery.oirs.database.repository;

import com.sourcery.oirs.config.mybatis.UuidTypeHandler;
import com.sourcery.oirs.database.entity.UserEntity;
import com.sourcery.oirs.model.Role;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Mapper
public interface UserRepository {

    @Select("SELECT * FROM employee e WHERE e.email = #{email}")
    @Results(value = {
            @Result(property = "id", column = "id", typeHandler = UuidTypeHandler.class),
            @Result(property = "fullName", column = "full_Name"),
            @Result(property = "email", column = "email"),
            @Result(property = "password", column = "password"),
            @Result(property = "phoneNumber", column = "phone_number"),
            @Result(property = "position", column = "position"),
            @Result(property = "avatar", column = "avatar"),
            @Result(property = "roles", column = "id", javaType = List.class, many = @Many(select = "getRolesById"))
    })
    Optional<UserEntity> findByEmail(@Param("email") String email);

    @Select("SELECT r.role_type FROM roles r WHERE r.employee_id = #{id}")
    List<Role> getRolesById(@Param("id") UUID id);
}

