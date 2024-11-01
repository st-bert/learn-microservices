package com.nbicocchi.inventory.runners;

import com.nbicocchi.inventory.persistence.model.Inventory;
import com.nbicocchi.inventory.persistence.model.Product;
import com.nbicocchi.inventory.persistence.repository.InventoryRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataLoader implements ApplicationRunner {
    private final InventoryRepository inventoryRepository;

    public DataLoader(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Product p;

        p = new Product("P-002", "Led Light", "A led light for bikes");
        inventoryRepository.save(new Inventory(p, 1000));

        p = new Product("P-003", "Led Light", "A led light for bikes");
        inventoryRepository.save(new Inventory(p, 2200));
    }
}
