package com.code.wpp_financas.services;

import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.repositories.CategoryRepository;
import com.code.wpp_financas.utils.TextUtils;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // sempre na mesma ordem (pelo id), para o número de cada categoria não mudar
    public List<Category> listAll() {
        return categoryRepository.findAll(Sort.by("id"));
    }

    // 1 - Alimentação\n2 - Transporte...
    public String menu() {
        List<Category> categories = listAll();
        StringBuilder menu = new StringBuilder();
        for(int i = 0; i < categories.size(); i++) {
            menu.append(i + 1).append(" - ").append(categories.get(i).getName()).append("\n");
        }
        return menu.toString().trim();
    }

    // aceita o número da lista ("1") ou o nome ("alimentação", "#Alimentacao")
    public Optional<Category> findByInput(String input) {
        if(input == null || input.isBlank()) {
            return Optional.empty();
        }
        String wanted = TextUtils.normalize(input).replace("#", "").trim();
        List<Category> categories = listAll();

        if(wanted.matches("\\d{1,3}")){
            int index = Integer.parseInt(wanted) - 1;
            return index >= 0 && index < categories.size() ?
                    Optional.of(categories.get(index)) : Optional.empty();
        }

        return categories.stream().filter(category -> TextUtils.normalize(category.getName()).equals(wanted)).findFirst();
    }
}
