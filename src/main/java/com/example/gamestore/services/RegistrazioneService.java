package com.example.gamestore.services;

import com.example.gamestore.dto.UtenteRegistrDto;
import com.example.gamestore.entities.Carrello;
import com.example.gamestore.entities.Utente;
import com.example.gamestore.repositories.CarrelloRepository;
import com.example.gamestore.repositories.UtenteRepository;
import com.example.gamestore.support.ResponseMessage;
import com.example.gamestore.support.authentication.Utils;
import com.example.gamestore.support.exceptions.ErroreNellaRegistrazioneUtenteException;
import com.example.gamestore.support.exceptions.UtenteNonValidoONonEsistente;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.gamestore.support.exceptions.ErroreLogoutException;

import java.util.*;


@Service
public class RegistrazioneService {

    // Repository per accedere agli utenti salvati nel DB
    private final UtenteRepository utenteRepository;
    // Repository per accedere ai carrelli salvati nel DB
    private final CarrelloRepository carrelloRepository;

    // Variabili lette dal file application.yaml (tramite @Value)
    @Value("${keycloak.server-url}")
    private String keycloakUrl;

    @Value("${keycloak.realm}")
    private String keycloakRealm;     // Nome del Realm in Keycloak (es: "gamestore")

    @Value("${keycloak.admin.username}")
    private String adminUsername;     // Username dell’admin Keycloak
    @Value("${keycloak.admin.password}")
    private String adminPassword;     // Password dell’admin Keycloak
    @Value("${keycloak.admin.client-id}")
    private String adminClientId;     // Client id usato dall’admin

    @Value("${keycloak.credentials.secret}")
    private String secret;            // Secret generato in Keycloak per il client gamestore-client

    @Value("${keycloak.client.id}")
    private String clientId;          // Client ID pubblico

    // Costruttore con injection dei repository
    public RegistrazioneService(UtenteRepository utenteRepository, CarrelloRepository carrelloRepository) {
        this.utenteRepository = utenteRepository;
        this.carrelloRepository = carrelloRepository;
    }

    /**
     * Metodo che registra un nuovo utente:
     * 1. Salva i dati nel DB locale
     * 2. Registra l'utente in Keycloak
     * 3. Se fallisce, viene fatto rollback della transazione DB
     */
    @Transactional(readOnly = false, rollbackFor = ErroreNellaRegistrazioneUtenteException.class)
    public ResponseEntity registraNuovoUtente(UtenteRegistrDto user) throws ErroreNellaRegistrazioneUtenteException {

        // Controllo: se DTO è null, lancio eccezione
        if (user == null)
            throw new ErroreNellaRegistrazioneUtenteException();

        // Creo entità Utente e la preparo per il DB
        Utente u = new Utente();
        u.setNome(user.firstName());        // nome preso dal DTO
        u.setCognome(user.lastName());      // cognome
        u.setEmail(user.email());           // email
        u.setOrdini(new ArrayList<>());     // lista ordini inizialmente vuota

        // Creo un carrello collegato al nuovo utente
        Carrello c = new Carrello();
        c.setUtente(u);                     // collego carrello → utente
        c.setAttivo(1);                     // flag per dire che è attivo
        c.setListaDettagliCarrello(new ArrayList<>()); // lista di prodotti nel carrello vuota

        // Salvo nel DB utente e carrello
        Utente utente_salvato = utenteRepository.save(u);
        carrelloRepository.save(c);

        // Creo istanza Keycloak admin client (mi connetto a Keycloak con credenziali admin)
        Keycloak keycloak = KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)        // URL server Keycloak
                .realm(keycloakRealm)          // realm da usare
                .username(adminUsername)       // admin username
                .password(adminPassword)       // admin password
                .clientId(adminClientId)       // admin client id
                .clientSecret(secret)          // secret del client
                .grantType(OAuth2Constants.PASSWORD) // tipo di grant usato
                .build();

        // Creo rappresentazione dell’utente da salvare in Keycloak
        UserRepresentation userk = new UserRepresentation();
        userk.setEnabled(true);               // attivo di default
        userk.setUsername(user.username());   // username preso dal DTO
        userk.setEmail(user.email());         // email
        userk.setFirstName(user.firstName()); // nome
        userk.setLastName(user.lastName());   // cognome
        userk.setEmailVerified(true);         // email segnata come verificata

        // Creo credenziali utente (password)
        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD); // tipo → password
        credentialRepresentation.setTemporary(false);                        // password non temporanea
        credentialRepresentation.setValue(user.password());                  // valore = password dal DTO

        // Inserisco credenziali in lista e le aggiungo all’utente
        List<CredentialRepresentation> list = new ArrayList<>();
        list.add(credentialRepresentation);
        userk.setCredentials(list);

        // Aggiungo attributi custom a Keycloak per collegare l’utente del DB con quello in Keycloak.
        // In particolare salvo l'id generato dal DB (idUtente) come attributo nell'account Keycloak,
        // così posso recuperarlo anche nei token JWT e sapere a quale record del DB corrisponde.
        // L’attributo "origin" è un esempio di metadato extra che può essere usato per tracciare
        // la provenienza o altre info personalizzate dell’utente.

        Integer idToSave = utente_salvato.getId();
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("idUtente", Collections.singletonList(idToSave.toString())); // salvo id DB come attributo
        attributes.put("origin", Arrays.asList("demo")); // attributo extra "origin"
        userk.setAttributes(attributes);

        // Mi collego alle API REST del realm Keycloak
        RealmResource realmResource = keycloak.realm(keycloakRealm);
        UsersResource usersRessource = realmResource.users();

        // Creo utente su Keycloak
        Response response = usersRessource.create(userk);

        // Se utente creato con successo (HTTP 201 CREATED)
        if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
            // Estraggo id generato da Keycloak
            String userId = response.getLocation().getPath().replaceAll(".*/([^/]+)$", "$1");

            // Recupero client di Keycloak (gamestore-client)
            ClientRepresentation clientRep = realmResource.clients().findByClientId(clientId).get(0);
            ClientResource clientResource = realmResource.clients().get(clientRep.getId());

            // Assegno ruolo "utente" al nuovo user
            RoleRepresentation userRole = clientResource.roles().get("utente").toRepresentation();
            usersRessource.get(userId).roles().clientLevel(clientResource.toRepresentation().getId())
                    .add(Collections.singletonList(userRole));

            // Ritorno utente salvato con HTTP 200
            return new ResponseEntity(utente_salvato, HttpStatus.OK);
        } else {
            // Se fallisce la creazione su Keycloak, lancio eccezione → rollback DB
            throw new ErroreNellaRegistrazioneUtenteException();
        }
    }

    // Metodo per logout (invalida refresh token su Keycloak)
    public ResponseEntity logoutUser(String refreshToken) throws ErroreLogoutException {
        try {
            Keycloak keycloak = KeycloakBuilder.builder()
                    .serverUrl(keycloakUrl)
                    .realm(keycloakRealm)
                    .clientId(clientId)
                    .clientSecret(secret)
                    .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                    .build();

            keycloak.tokenManager().invalidate(refreshToken);

            return new ResponseEntity<>(new ResponseMessage("LOGOUT EFFETTUATO CON SUCCESSO"), HttpStatus.OK);
        } catch (Exception e) {
            throw new ErroreLogoutException();
        }
    }

    // Metodo che trova utente nel DB partendo dall'id presente nel token JWT
    @Transactional(readOnly = true)
    public Utente trovaUtente() throws UtenteNonValidoONonEsistente {
        Integer idUtente= Utils.getIdUtente();  // estraggo idUtente dal token JWT
        if(idUtente==null){
            return null;
        }
        Optional<Utente> u = utenteRepository.findById(idUtente);
        if (u.isPresent()) {
            return u.get();
        }
        else {
            throw new UtenteNonValidoONonEsistente();
        }
    }
}