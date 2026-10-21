package ru.pool.poolmanagement.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.pool.poolmanagement.dto.client.ClientDetailsResponse;
import ru.pool.poolmanagement.dto.client.ClientListResponse;
import ru.pool.poolmanagement.dto.client.ClientRequest;
import ru.pool.poolmanagement.service.ClientService;

import java.util.List;

@RestController
@RequestMapping("/api/v0/pool/clients")
public class ClientController {

    private final ClientService clientService;

    public ClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    // Получение списка клиентов (+ поиск по имени)
    @GetMapping
    public List<ClientListResponse> getAllClients(@RequestParam(required = false) String name) {
        return clientService.getAllClients(name);
    }

    // Получение данных о клиенте
    @GetMapping("/{id}")
    public ClientDetailsResponse getClientById(@PathVariable Long id) {
        return clientService.getClientById(id);
    }

    // Добавление нового клиента
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientDetailsResponse createClient(@Valid @RequestBody ClientRequest request) {
        return clientService.createClient(request);
    }

    // Обновление данных о клиенте (PATCH)
    @PatchMapping("/{id}")
    public ClientDetailsResponse patchClient(@PathVariable Long id, @RequestBody ClientRequest request) {
        return clientService.patchClient(id, request);
    }
}