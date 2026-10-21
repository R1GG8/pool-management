package ru.pool.poolmanagement.service;

import org.springframework.stereotype.Service;
import ru.pool.poolmanagement.dto.client.ClientDetailsResponse;
import ru.pool.poolmanagement.dto.client.ClientListResponse;
import ru.pool.poolmanagement.dto.client.ClientRequest;
import ru.pool.poolmanagement.exception.NotFoundException;
import ru.pool.poolmanagement.model.Client;
import ru.pool.poolmanagement.repository.ClientRepository;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    public List<ClientListResponse> getAllClients(String searchName) {
        List<Client> clients;
        if (searchName != null && !searchName.isBlank()) {
            clients = clientRepository.findByNameContaining(searchName);
        } else {
            clients = clientRepository.findAll();
        }

        return clients.stream()
                .map(c -> new ClientListResponse(c.getId(), c.getName()))
                .toList();
    }

    public ClientDetailsResponse getClientById(Long id) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Клиент с id " + id + " не найден"));

        return toDetailsResponse(client);
    }

    public ClientDetailsResponse createClient(ClientRequest request) {
        Client client = new Client(null, request.name(), request.phone(), request.email());
        Client saved = clientRepository.save(client);
        return toDetailsResponse(saved);
    }

    public ClientDetailsResponse patchClient(Long id, ClientRequest request) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Клиент с id " + id + " не найден"));

        if (request.name() != null && !request.name().isBlank()) {
            client.setName(request.name());
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            client.setPhone(request.phone());
        }
        if (request.email() != null && !request.email().isBlank()) {
            client.setEmail(request.email());
        }

        clientRepository.update(client);
        return toDetailsResponse(client);
    }

    private ClientDetailsResponse toDetailsResponse(Client c) {
        return new ClientDetailsResponse(c.getId(), c.getName(), c.getPhone(), c.getEmail());
    }
}