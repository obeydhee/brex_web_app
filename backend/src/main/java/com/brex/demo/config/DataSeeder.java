package com.brex.demo.config;

import com.brex.demo.model.Item;
import com.brex.demo.repository.ItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final ItemRepository itemRepository;

    public DataSeeder(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public void run(String... args) {
        if (itemRepository.count() == 0) {
            itemRepository.save(new Item("Welcome", "This item was seeded on first startup."));
            itemRepository.save(new Item("SQLite", "Data is persisted to a local SQLite file."));
            itemRepository.save(new Item("Spring Boot", "The backend is powered by Spring Boot + Gradle."));
        }
    }
}
