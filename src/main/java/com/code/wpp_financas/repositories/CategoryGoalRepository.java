package com.code.wpp_financas.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.code.wpp_financas.models.CategoryGoal;

public interface CategoryGoalRepository extends JpaRepository<CategoryGoal, Long> {

    Optional<CategoryGoal> findByUserIdAndCategoryId(Long userId, Long categoryId);

    // todas as metas do usuário, já com a categoria carregada
    @Query("select g from CategoryGoal g join fetch g.category where g.user.id = :userId order by g.category.id")
    List<CategoryGoal> findAllByUser(@Param("userId") Long userId);
}