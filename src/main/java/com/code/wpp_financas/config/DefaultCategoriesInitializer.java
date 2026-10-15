package com.code.wpp_financas.config;

import com.code.wpp_financas.models.Category;
import com.code.wpp_financas.repositories.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

// Cria as categorias padrão na primeira vez que a aplicação sobe
@Component
public class DefaultCategoriesInitializer implements CommandLineRunner {
    private static final List<String> DEFAULTS = List.of(
            "Alimentação",
            "Transporte",
            "Saúde",
            "Educação",
            "Lazer",
            "Moradia",
            "Roupas",
            "Serviços",
            "Investimentos",
            "Outros"
    );

    private final CategoryRepository categoryRepository;

    public DefaultCategoriesInitializer(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public void run(String... args){
        for(String name : DEFAULTS) {
            if(categoryRepository.findByNameIgnoreCase(name).isEmpty()){
                categoryRepository.save(new Category(name));
            }
        }
    }
}
