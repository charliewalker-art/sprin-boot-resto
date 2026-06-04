# Monappweb — Rapport d'Architecture et Fonctionnel
> Dernière mise à jour : après ajout du module Commande/Paiement

---

## 1. Résumé exécutif

Monappweb est une application backend **Spring Boot** destinée à la gestion complète d'un restaurant : gestion du personnel (utilisateurs), du menu (plats), des tables (statuts, appel serveur), upload d'images, et désormais **gestion complète du cycle de vie des commandes et des paiements**.

L'application expose des API REST, utilise **Spring Data JPA** pour la persistance (PostgreSQL), et met en œuvre une authentification par **JWT**. Les workflows principaux sont :

- Authentification (login → JWT)
- CRUD et opérations métier sur les plats et tables
- Gestion des comptes staff
- Upload d'images pour les plats
- **Création, suivi et clôture des commandes (nouveau)**
- **Encaissement et gestion des paiements (nouveau)**

---

## 2. Architecture globale & packages

```
com.example.monappweb
├── controller         → Exposition API REST (endpoints HTTP)
├── service            → Logique métier et règles de gestion
├── repository         → Accès base de données via Spring Data JPA
├── entity             → Entités JPA + Enums (modèle de domaine)
├── dto                → Objets de transfert Request/Response
├── security           → JWT : configuration, filtre, utilitaires
├── exception          → Handler global d'exceptions
└── config             → Configurations applicatives (CORS, etc.)
```

### Principe général (flux d'une requête)

```
Client HTTP
    │
    ▼
[JwtFilter]  ← vérifie le token Authorization: Bearer <token>
    │
    ▼
[Controller] ← reçoit la requête, valide les DTOs (@Valid)
    │
    ▼
[Service]    ← applique les règles métier, transitions d'état
    │
    ▼
[Repository] ← exécute les requêtes SQL via Spring Data JPA
    │
    ▼
[PostgreSQL] ← stocke les données
```

---

## 3. Modèle de données complet (Toutes les entités)

### Vue d'ensemble des relations

```
[Utilisateur] (Staff)
      │
      ├── (sert / annule / encaisse)
      │
      ▼
[TableRestaurant] ◄──── [Commande] ◄─── [DetailCommande] ◄─── [Plat]
                              │
                              │ (1:1)
                              ▼
                          [Paiement]
```

---

### 3.1 Utilisateur (`table: utilisateurs`)

Représente un membre du personnel. Implémente `UserDetails` de Spring Security pour l'authentification JWT.

| Champ | Type | Contrainte | Description |
|-------|------|------------|-------------|
| `id` | Long | PK, auto-généré | Identifiant unique |
| `username` | String | UNIQUE, NOT NULL | Identifiant de connexion |
| `password` | String | NOT NULL | Mot de passe hashé BCrypt |
| `nom` | String | NOT NULL | Nom de famille |
| `prenom` | String | NOT NULL | Prénom |
| `role` | Enum `Role` | NOT NULL | Rôle dans le système |
| `actif` | Boolean | NOT NULL | Permet de désactiver sans supprimer |

**Remarque importante :** `actif = false` coupe l'accès sans perdre l'historique des commandes liées à cet utilisateur (les `ManyToOne` vers Utilisateur dans Commande et Paiement restent intacts).

**Méthode `getAuthorities()`** retourne `ROLE_<NOM_DU_ROLE>` (ex: `ROLE_MANAGER`), utilisé par Spring Security pour les `@PreAuthorize`.

---

### 3.2 Plat (`table: plats`)

Représente un article du menu. Géré par le MANAGER, modifiable en disponibilité par la CUISINIERE.

| Champ | Type | Contrainte | Description |
|-------|------|------------|-------------|
| `id` | Long | PK, auto-généré | Identifiant unique |
| `nom` | String | NOT NULL | Nom du plat affiché au client |
| `description` | String | nullable | Description courte |
| `prix` | BigDecimal | NOT NULL | Prix avec 2 décimales (ex: 12.50) |
| `categorie` | Enum `Categorie` | NOT NULL | ENTREE, PLAT, DESSERT, BOISSON |
| `disponible` | Boolean | NOT NULL | false = retiré du menu QR en temps réel |
| `allergenes` | String | nullable | Liste des allergènes (texte libre) |
| `quantitePerdueJour` | Integer | NOT NULL, défaut 0 | Compteur de gaspillage, remis à 0 chaque matin |
| `imageUrl` | String | nullable | URL de l'image uploadée via `/api/upload/image` |

---

### 3.3 TableRestaurant (`table: tables_restaurant`)

Représente une table physique de la salle. Le QR code est lié à cette table.

| Champ | Type | Contrainte | Description |
|-------|------|------------|-------------|
| `id` | Long | PK, auto-généré | Identifiant unique |
| `numeroTable` | Integer | UNIQUE, NOT NULL | Numéro affiché sur la table |
| `statut` | Enum `StatutTable` | NOT NULL | État actuel de la table |
| `qrCodeUrl` | String | nullable | URL du QR code généré |
| `appelServeurActif` | Boolean | NOT NULL | `true` quand le client a cliqué "Appeler le serveur" |
| `heureAppel` | LocalDateTime | nullable | Horodatage de l'appel serveur |

**Transition automatique :** lors du paiement d'une commande, la table associée passe automatiquement en `EN_COURS_DE_NETTOYAGE` (géré dans `PaiementService`).

---

### 3.4 Commande (`table: commandes`) ⭐ NOUVEAU

Entité centrale du système. Regroupe toutes les informations d'une commande, de sa création jusqu'à son paiement ou annulation.

| Champ | Type | Contrainte | Description |
|-------|------|------------|-------------|
| `id` | Long | PK, auto-généré | Identifiant unique |
| `typeCommande` | Enum `TypeCommande` | NOT NULL | Origine de la commande |
| `statut` | Enum `StatutCommande` | NOT NULL | État actuel dans le cycle de vie |
| `servicePeriode` | Enum `ServicePeriode` | NOT NULL | MIDI (6h-15h) ou SOIR (15h-6h) — calculé automatiquement |
| `table` | ManyToOne → TableRestaurant | nullable | Null si commande à emporter |
| `serveur` | ManyToOne → Utilisateur | nullable | Null si commande QR autonome |
| `nomClientRetrait` | String | nullable | Nom du client pour les commandes à emporter |
| `dateCreation` | LocalDateTime | NOT NULL | Rempli automatiquement par `@PrePersist` |
| `tempsAttenteEstime` | Integer | nullable | Estimation en minutes |
| `details` | OneToMany → DetailCommande | CascadeAll | Lignes du panier |
| **Bloc annulation** | | | |
| `annulePar` | ManyToOne → Utilisateur | nullable | Qui a annulé |
| `motifAnnulation` | String | nullable | Raison obligatoire à l'annulation |
| `dateAnnulation` | LocalDateTime | nullable | Horodatage de l'annulation |
| **Bloc évaluation** | | | |
| `noteSatisfaction` | Integer (1-5) | nullable | Note donnée par le client |
| `commentaireClient` | String (500 chars) | nullable | Commentaire libre |

**`@PrePersist` :** initialise `dateCreation`, `statut = CREEE`, et calcule `servicePeriode` automatiquement selon l'heure.

---

### 3.5 DetailCommande (`table: details_commande`) ⭐ NOUVEAU

Représente une ligne du panier : un plat + une quantité + une note de cuisson.

| Champ | Type | Contrainte | Description |
|-------|------|------------|-------------|
| `id` | Long | PK, auto-généré | Identifiant unique |
| `commande` | ManyToOne → Commande | NOT NULL | Commande parente |
| `plat` | ManyToOne → Plat | NOT NULL | Plat commandé |
| `quantite` | Integer | NOT NULL, min 1 | Nombre d'exemplaires |
| `noteClient` | String (300 chars) | nullable | Instruction de cuisson (ex: "sans oignon", "bien cuit") |

**Cascade :** `CascadeType.ALL` + `orphanRemoval = true` — si on supprime la commande, tous ses détails sont supprimés automatiquement.

---

### 3.6 Paiement (`table: paiements`) ⭐ NOUVEAU

Enregistre l'encaissement d'une commande. Relation 1:1 avec Commande — une commande ne peut être encaissée qu'une seule fois.

| Champ | Type | Contrainte | Description |
|-------|------|------------|-------------|
| `id` | Long | PK, auto-généré | Identifiant unique |
| `commande` | OneToOne → Commande | NOT NULL, UNIQUE | La commande encaissée |
| `caissier` | ManyToOne → Utilisateur | NOT NULL | Qui a encaissé |
| `montantTotal` | BigDecimal (10,2) | NOT NULL | Montant total encaissé |
| `pourboire` | BigDecimal (10,2) | NOT NULL, défaut 0 | Pourboire éventuel |
| `modePaiement` | Enum `ModePaiement` | NOT NULL | Mode utilisé |
| `datePaiement` | LocalDateTime | NOT NULL | Rempli par `@PrePersist` |

---

## 4. Enums complets

### Enums existants

| Enum | Valeurs |
|------|---------|
| `Role` | `SERVEUR`, `CUISINIERE`, `CAISSIER`, `MANAGER`, `RESPONSABLE_PERSONNEL` |
| `Categorie` | `ENTREE`, `PLAT`, `DESSERT`, `BOISSON` |
| `StatutTable` | `LIBRE`, `OCCUPEE`, `EN_COURS_DE_NETTOYAGE`, `RESERVEE` |

### Nouveaux enums ⭐

| Enum | Valeurs | Description |
|------|---------|-------------|
| `TypeCommande` | `SUR_PLACE_QR`, `SUR_PLACE_SERVEUR`, `A_EMPORTER` | Origine de la commande |
| `StatutCommande` | `CREEE`, `EN_ATTENTE_CUISINE`, `EN_PREPARATION`, `PRETE`, `SERVIE`, `EN_ATTENTE_PAIEMENT`, `PAYEE`, `ANNULEE` | Cycle de vie complet |
| `ServicePeriode` | `MIDI`, `SOIR` | Pour les rapports de fin de service |
| `ModePaiement` | `ESPECES`, `CARTE`, `MOBILE_MONEY` | Mode d'encaissement |

---

## 5. Cycle de vie d'une commande

```
[CREEE] ──── valider() ────► [EN_ATTENTE_CUISINE]
                                      │
                              commencer() — cuisinière
                                      │
                                      ▼
                              [EN_PREPARATION]
                                      │
                              marquerPrete() — cuisinière
                                      │
                                      ▼
                                  [PRETE]
                                      │
                              marquerServie() — serveur
                                      │
                                      ▼
                                  [SERVIE]
                                      │
                              demanderAddition() — client
                                      │
                                      ▼
                          [EN_ATTENTE_PAIEMENT]
                                      │
                              encaisser() — caissier
                                      │
                                      ▼
                                  [PAYEE]
                                      │
                         (table → EN_COURS_DE_NETTOYAGE)

Annulation possible depuis : CREEE ou EN_ATTENTE_CUISINE
(impossible depuis EN_PREPARATION, PRETE, SERVIE, PAYEE)
```

**Règle servicePeriode :** calculée automatiquement à la création :
- heure >= 6h et < 15h → `MIDI`
- heure >= 15h ou < 6h → `SOIR`

---

## 6. Sécurité

### Composants

**`SecurityConfig.java`** — configure `SecurityFilterChain`, CORS, session stateless, ajoute `JwtFilter`.

> ⚠️ **Point d'attention :** la configuration actuelle utilise `anyRequest().permitAll()` — toutes les requêtes sont publiques par défaut. Les `@PreAuthorize` présents dans les controllers ne s'appliquent pas encore car `@EnableMethodSecurity` n'est pas activé. **À corriger avant mise en production.**

**`JwtUtil.java`** — génère et valide les JWT depuis `jwt.secret` et `jwt.expiration` (`application.properties`). Méthodes : `generateToken`, `extractUsername`, `extractRole`, `isTokenValid`.

**`JwtFilter.java`** — intercepte chaque requête, lit le header `Authorization: Bearer <token>`, valide le token, charge les `UserDetails` et injecte l'authentification dans le `SecurityContext`.

**`UserDetailsServiceImpl.java`** — charge l'entité `Utilisateur` depuis `UserRepository` pour la vérification du token.

### Flux d'authentification

```
1. POST /api/auth/login
   └─► AuthenticationManager.authenticate()
         └─► UserDetailsService.loadUserByUsername()
               └─► PasswordEncoder.matches()
                     └─► OK → JwtUtil.generateToken() → retourné au client

2. Requêtes suivantes :
   └─► Header: Authorization: Bearer <token>
         └─► JwtFilter valide → SecurityContext alimenté
               └─► @PreAuthorize vérifie le rôle (si @EnableMethodSecurity activé)
```

### Matrice des droits (telle que définie dans les controllers)

| Endpoint | Rôle requis |
|----------|------------|
| `POST /api/utilisateurs` | RESPONSABLE_PERSONNEL |
| `GET /api/utilisateurs` | RESPONSABLE_PERSONNEL |
| `GET /api/plats` | MANAGER |
| `POST /api/plats` | MANAGER |
| `PATCH /api/plats/{id}/toggle` | MANAGER, CUISINIERE |
| `PATCH /api/plats/{id}/perte/{q}` | CUISINIERE |
| `PATCH /api/commandes/{id}/commencer` | CUISINIERE, MANAGER |
| `PATCH /api/commandes/{id}/prete` | CUISINIERE, MANAGER |
| `PATCH /api/commandes/{id}/servie` | SERVEUR, MANAGER |
| `GET /api/commandes/retard` | CUISINIERE, MANAGER |
| `GET /api/commandes/annulations` | MANAGER |
| `POST /api/paiements` | CAISSIER, MANAGER |
| `GET /api/paiements/stats` | MANAGER |

---

## 7. Endpoints API complets

### Auth

| Méthode | Endpoint | Description | Corps |
|---------|----------|-------------|-------|
| POST | `/api/auth/login` | Connexion, retourne JWT | `LoginRequest` |

### Utilisateurs (staff)

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/utilisateurs` | Créer un compte staff |
| GET | `/api/utilisateurs` | Lister le personnel |
| PATCH | `/api/utilisateurs/{id}/desactiver` | Activer / désactiver un compte |
| PATCH | `/api/utilisateurs/profil` | Modifier son propre profil |

### Plats

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/plats/menu` | Menu public (QR code, plats disponibles) |
| GET | `/api/plats` | Tous les plats (MANAGER) |
| POST | `/api/plats` | Créer un plat |
| PUT | `/api/plats/{id}` | Modifier un plat |
| PATCH | `/api/plats/{id}/toggle` | Activer/désactiver la disponibilité |
| PATCH | `/api/plats/{id}/perte/{quantite}` | Déclarer une perte (gaspillage) |
| DELETE | `/api/plats/{id}` | Supprimer un plat |

### Tables

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/tables` | Créer une table |
| GET | `/api/tables` | Lister toutes les tables |
| GET | `/api/tables/statut/{statut}` | Filtrer par statut |
| PATCH | `/api/tables/{id}/statut/{statut}` | Changer le statut |
| PATCH | `/api/tables/appel/{numeroTable}` | Client appelle le serveur |
| PATCH | `/api/tables/{id}/acquitter` | Acquitter l'appel |
| DELETE | `/api/tables/{id}` | Supprimer une table |

### Upload

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/upload/image` | Upload d'image, retourne `{ "imageUrl": "..." }` |

### Commandes ⭐ NOUVEAU

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/api/commandes` | Tous | Créer une commande |
| GET | `/api/commandes/{id}` | Tous | Détail d'une commande |
| GET | `/api/commandes?statut=EN_ATTENTE_CUISINE` | Tous | Lister par statut (écran cuisine, caissier...) |
| PATCH | `/api/commandes/{id}/valider` | Tous | CREEE → EN_ATTENTE_CUISINE |
| PATCH | `/api/commandes/{id}/commencer` | CUISINIERE, MANAGER | EN_ATTENTE_CUISINE → EN_PREPARATION |
| PATCH | `/api/commandes/{id}/prete` | CUISINIERE, MANAGER | EN_PREPARATION → PRETE |
| PATCH | `/api/commandes/{id}/servie` | SERVEUR, MANAGER | PRETE → SERVIE |
| PATCH | `/api/commandes/{id}/addition` | Tous | SERVIE → EN_ATTENTE_PAIEMENT |
| PATCH | `/api/commandes/{id}/annuler?annuleParId=X` | Tous | Annuler avec motif obligatoire |
| PATCH | `/api/commandes/{id}/evaluer` | Tous | Ajouter note (1-5) + commentaire |
| GET | `/api/commandes/retard?seuilMinutes=15` | CUISINIERE, MANAGER | Commandes en cuisine depuis > N min |
| GET | `/api/commandes/annulations` | MANAGER | Journal complet des annulations |

### Paiements ⭐ NOUVEAU

| Méthode | Endpoint | Rôle | Description |
|---------|----------|------|-------------|
| POST | `/api/paiements` | CAISSIER, MANAGER | Encaisser une commande |
| GET | `/api/paiements/commande/{commandeId}` | CAISSIER, MANAGER | Paiement d'une commande |
| GET | `/api/paiements/aujourdhui` | CAISSIER, MANAGER | Tous les paiements du jour |
| GET | `/api/paiements/stats` | MANAGER | Total CA + pourboires du jour |

---

## 8. DTOs — Objets de transfert

### DTOs existants

| DTO | Sens | Usage |
|-----|------|-------|
| `LoginRequest` | → API | `username`, `password` |
| `LoginResponse` | ← API | `token`, infos utilisateur |
| `PlatRequest` | → API | Création / modification d'un plat |
| `PlatResponse` | ← API | Données complètes d'un plat |
| `TableRequest` | → API | Création d'une table |
| `TableResponse` | ← API | Données d'une table |
| `UtilisateurRequest` | → API | Création d'un compte staff |
| `UtilisateurResponse` | ← API | Données d'un utilisateur |
| `UpdateProfilRequest` | → API | Modification du propre profil |

### Nouveaux DTOs ⭐

| DTO | Sens | Champs clés |
|-----|------|-------------|
| `CommandeRequest` | → API | `typeCommande`, `tableId`, `serveurId`, `nomClientRetrait`, `details[]` |
| `DetailCommandeRequest` | → API (imbriqué) | `platId`, `quantite`, `noteClient` |
| `CommandeResponse` | ← API | Toutes les données + détails + total calculé + infos annulation/évaluation |
| `DetailCommandeResponse` | ← API (imbriqué) | `platNom`, `platPrix`, `quantite`, `noteClient`, `sousTotal` |
| `PaiementRequest` | → API | `commandeId`, `modePaiement`, `montantTotal`, `pourboire`, `caissierId` |
| `PaiementResponse` | ← API | `caissierNomComplet`, `montantTotal`, `pourboire`, `modePaiement`, `datePaiement` |
| `AnnulationRequest` | → API | `motifAnnulation` (obligatoire) |
| `EvaluationRequest` | → API | `noteSatisfaction` (1-5, obligatoire), `commentaireClient` |

**Calcul automatique dans `CommandeResponse` :**
- `sousTotal` par ligne = `platPrix × quantite`
- `montantTotal` de la commande = somme de tous les `sousTotal`

---

## 9. Repositories — Requêtes personnalisées

### Existants

| Repository | Méthodes notables |
|-----------|-------------------|
| `UserRepository` | `findByUsername()` |
| `PlatRepository` | `findByDisponibleTrue()` |
| `TableRepository` | `findByStatut()`, `findByNumeroTable()` |

### Nouveaux ⭐

**`CommandeRepository`**

| Méthode | Description |
|---------|-------------|
| `findByStatutOrderByDateCreationAsc(statut)` | Écran cuisine, écran caissier |
| `findByTableIdAndStatutNot(tableId, statut)` | Commandes actives d'une table |
| `findCommandesEnRetard(seuil)` | Alerte anti-oubli (>15 min en cuisine) — requête `@Query` |
| `findByServicePeriodeAndDateCreationBetween(...)` | Rapport MIDI / SOIR |
| `findByStatutOrderByDateAnnulationDesc(ANNULEE)` | Journal des annulations |

**`PaiementRepository`**

| Méthode | Description |
|---------|-------------|
| `findByCommandeId(commandeId)` | Paiement d'une commande spécifique |
| `findByCaissierId(caissierId)` | Paiements d'un caissier |
| `sumMontantBetween(debut, fin)` | CA total sur une période (`@Query` SUM) |
| `sumPourboiseBetween(debut, fin)` | Total pourboires sur une période |
| `findByDatePaiementBetween(debut, fin)` | Rapport journalier |

---

## 10. Services — Logique métier

### Existants

| Service | Responsabilités |
|---------|----------------|
| `AuthService` | Login, génération JWT |
| `UtilisateurService` | CRUD staff, activation/désactivation |
| `PlatService` | CRUD plats, toggle disponibilité, déclaration pertes |
| `TableService` | CRUD tables, gestion statuts, appel serveur |
| `UploadService` | Validation et stockage d'images |
| `UserDetailsServiceImpl` | Chargement utilisateur pour Spring Security |

### Nouveaux ⭐

**`CommandeService`**

| Méthode | Transition | Règles métier |
|---------|-----------|--------------|
| `creerCommande()` | → CREEE | Vérifie que chaque plat est `disponible = true` avant d'accepter |
| `validerCommande()` | CREEE → EN_ATTENTE_CUISINE | Statut requis : CREEE |
| `commencerPreparation()` | EN_ATTENTE_CUISINE → EN_PREPARATION | Statut requis : EN_ATTENTE_CUISINE |
| `marquerPrete()` | EN_PREPARATION → PRETE | Statut requis : EN_PREPARATION |
| `marquerServie()` | PRETE → SERVIE | Statut requis : PRETE |
| `demanderAddition()` | SERVIE → EN_ATTENTE_PAIEMENT | Statut requis : SERVIE |
| `annulerCommande()` | → ANNULEE | Interdit si EN_PREPARATION, PRETE, SERVIE ou PAYEE |
| `evaluerCommande()` | — | Seulement si statut = PAYEE |
| `getCommandesEnRetard()` | — | Retourne les commandes EN_ATTENTE_CUISINE depuis > N minutes |
| `getJournalAnnulations()` | — | Toutes les commandes ANNULEE triées par date |

**`PaiementService`**

| Méthode | Règles métier |
|---------|--------------|
| `encaisser()` | Vérifie statut EN_ATTENTE_PAIEMENT + unicité (pas de double encaissement) + passe commande à PAYEE + table à EN_COURS_DE_NETTOYAGE automatiquement |
| `getPaiementsDuJour()` | Filtre entre minuit et 23h59 du jour courant |
| `getTotalEncaisseAujourdhui()` | Agrégat SUM — retourne 0 si aucun paiement |
| `getTotalPourboiresAujourdhui()` | Agrégat SUM — retourne 0 si aucun pourboire |

---

## 11. Fichiers de configuration & ressources

### `application.properties`

Toutes les valeurs sensibles sont externalisées via variables d'environnement :

| Variable d'env | Usage |
|---------------|-------|
| `DB_HOST` | Hôte PostgreSQL |
| `DB_PORT` | Port PostgreSQL (défaut : 5432) |
| `DB_NAME` | Nom de la base de données |
| `DB_USERNAME` | Utilisateur PostgreSQL |
| `DB_PASSWORD` | Mot de passe PostgreSQL |
| `JWT_SECRET` | Clé secrète de signature des tokens JWT |
| `JWT_EXPIRATION` | Durée de validité du token (en ms) |
| `UPLOAD_DIR` | Dossier local pour les images uploadées |
| `APP_BASE_URL` | URL de base pour construire les URLs d'images |

### `data.sql`

Seed initial : crée un compte admin `charlie.admin` avec le rôle `RESPONSABLE_PERSONNEL` et un mot de passe hashé BCrypt. Permet de bootstrapper l'application sans compte staff.

### Dossier uploads

`uploads/plats/` — contient les images des plats uploadées via `/api/upload/image`.

---

## 12. Structure des fichiers sources (chemins complets)

```
src/main/java/com/example/monappweb/
│
├── MonappwebApplication.java
│
├── entity/
│   ├── Utilisateur.java
│   ├── Plat.java
│   ├── TableRestaurant.java
│   ├── Commande.java              ⭐ NOUVEAU
│   ├── DetailCommande.java        ⭐ NOUVEAU
│   ├── Paiement.java              ⭐ NOUVEAU
│   ├── Role.java
│   ├── Categorie.java
│   ├── StatutTable.java
│   ├── TypeCommande.java          ⭐ NOUVEAU
│   ├── StatutCommande.java        ⭐ NOUVEAU
│   ├── ServicePeriode.java        ⭐ NOUVEAU
│   └── ModePaiement.java          ⭐ NOUVEAU
│
├── dto/
│   ├── LoginRequest.java
│   ├── LoginResponse.java
│   ├── PlatRequest.java / PlatResponse.java
│   ├── TableRequest.java / TableResponse.java
│   ├── UtilisateurRequest.java / UtilisateurResponse.java
│   ├── UpdateProfilRequest.java
│   ├── CommandeRequest.java       ⭐ NOUVEAU
│   ├── CommandeResponse.java      ⭐ NOUVEAU
│   ├── DetailCommandeRequest.java ⭐ NOUVEAU
│   ├── DetailCommandeResponse.java⭐ NOUVEAU
│   ├── PaiementRequest.java       ⭐ NOUVEAU
│   ├── PaiementResponse.java      ⭐ NOUVEAU
│   ├── AnnulationRequest.java     ⭐ NOUVEAU
│   └── EvaluationRequest.java     ⭐ NOUVEAU
│
├── repository/
│   ├── UserRepository.java
│   ├── PlatRepository.java
│   ├── TableRepository.java
│   ├── CommandeRepository.java    ⭐ NOUVEAU
│   └── PaiementRepository.java    ⭐ NOUVEAU
│
├── service/
│   ├── AuthService.java
│   ├── UtilisateurService.java
│   ├── PlatService.java
│   ├── TableService.java
│   ├── UploadService.java
│   ├── UserDetailsServiceImpl.java
│   ├── CommandeService.java       ⭐ NOUVEAU
│   └── PaiementService.java       ⭐ NOUVEAU
│
├── controller/
│   ├── AuthController.java
│   ├── UtilisateurController.java
│   ├── PlatController.java
│   ├── TableController.java
│   ├── UploadController.java
│   ├── CommandeController.java    ⭐ NOUVEAU
│   └── PaiementController.java    ⭐ NOUVEAU
│
├── security/
│   ├── SecurityConfig.java
│   ├── JwtUtil.java
│   └── JwtFilter.java
│
├── exception/
│   ├── GlobalExceptionHandler.java
│   └── ErrorResponse.java
│
└── config/
    └── WebConfig.java

src/main/resources/
├── application.properties
└── data.sql
```

---

## 13. Recommandations & prochaines étapes

### Priorité haute — Sécurité

1. **Activer `@EnableMethodSecurity`** sur `SecurityConfig` ou `MonappwebApplication` pour que les `@PreAuthorize` dans les controllers prennent effet.
2. **Remplacer `anyRequest().permitAll()`** par des règles adaptées :
    - `/api/auth/**` → public
    - `/api/plats/menu` → public
    - Tout le reste → authentifié
3. **Vérifier `JwtUtil.getClaims()`** — adapter l'API JJWT à la version déclarée dans `pom.xml`.

### Priorité haute — Qualité

4. **Remplacer les `RuntimeException` brutes** par des exceptions métier spécifiques : `ResourceNotFoundException` (404), `ConflictException` (409), `BadRequestException` (400) — et les gérer dans `GlobalExceptionHandler`.
5. **Valider `CommandeRequest`** : si `typeCommande = SUR_PLACE_QR` ou `SUR_PLACE_SERVEUR` alors `tableId` est obligatoire ; si `A_EMPORTER` alors `nomClientRetrait` est obligatoire. Cette validation croisée doit être ajoutée dans `CommandeService.creerCommande()`.

### Priorité moyenne — Fonctionnalités

6. **Ajouter le script SQL de migration** (`V2__add_commande_tables.sql`) pour créer les tables `commandes`, `details_commande`, `paiements` via Flyway ou Liquibase.
7. **Notifications temps réel** — implémenter WebSocket (Spring WebSocket + STOMP) pour notifier :
    - La cuisine quand une nouvelle commande arrive
    - Le serveur quand une commande est PRETE
    - Le caissier quand une commande passe EN_ATTENTE_PAIEMENT
8. **Rapport de fin de service** — endpoint `GET /api/rapports/service?periode=MIDI&date=2025-01-15` calculant CA, plats phares, temps moyen d'attente.

### Priorité basse — Outillage

9. **Ajouter Swagger/OpenAPI** (`springdoc-openapi`) pour documenter automatiquement tous les endpoints.
10. **Ajouter des tests** unitaires (`CommandeService`, `PaiementService`) et d'intégration (`CommandeController`).
11. **Auditer `pom.xml`** pour les vulnérabilités et fixer les versions critiques.
12. **Ajouter un README** avec instructions de démarrage local, variables d'environnement requises et exemples d'appels `curl`.
13. **Logging structuré** avec niveaux adaptés (`INFO` pour les transitions de statut, `WARN` pour les annulations, `ERROR` pour les exceptions).

---

## Mise à jour — WebSocket & Corrections

Cette section documente les modifications récentes apportées à cinq fichiers du projet pour activer les notifications WebSocket/STOMP et corriger l'API d'authentification.

1. `src/main/java/com/example/monappweb/config/WebSocketConfig.java` (nouveau fichier dans `config/`)
   - Ajout de la configuration Spring WebSocket + STOMP.
   - Endpoint de connexion configuré : `/ws` avec fallback SockJS.
   - Broker simple configuré sur `/topic`.
   - Préfixe des destinations applicatives : `/app`.

2. `src/main/java/com/example/monappweb/service/CommandeService.java`
   - `SimpMessagingTemplate` injecté pour publier des messages STOMP.
   - Méthode privée `notifier(String destination, Object payload)` ajoutée qui envoie les notifications vers `/topic/commandes` (ou destination fournie).
   - `notifier()` appelée après chaque transition de statut dans les méthodes : `creerCommande`, `validerCommande`, `commencerPreparation`, `marquerPrete`, `marquerServie`, `demanderAddition`, `annulerCommande`, `evaluerCommande` afin de diffuser l'état courant de la commande aux abonnés.
   - Méthode `getToutesLesCommandes()` ajoutée, utilisant `commandeRepository.findAllByOrderByDateCreationAsc()` pour lister toutes les commandes triées par date de création.

3. `src/main/java/com/example/monappweb/service/PaiementService.java`
   - `SimpMessagingTemplate` injecté pour envoyer des notifications sur les événements de paiement.
   - Appel à `notifier()` (ou publication via `SimpMessagingTemplate`) effectué après `encaisser()` pour diffuser l'événement de paiement vers `/topic/commandes` (ou topic dédié aux paiements si nécessaire).

4. `src/main/java/com/example/monappweb/dto/LoginResponse.java`
   - Ajout du champ `Long id` comme premier champ du DTO `LoginResponse` afin que le frontend puisse récupérer l'ID de l'utilisateur authentifié rapidement.

5. `src/main/java/com/example/monappweb/service/AuthService.java`
   - Mise à jour de l'appel au constructeur de `LoginResponse` pour passer `utilisateur.getId()` comme premier argument (respectant l'ordre des champs modifié dans le DTO).

Ces ajouts permettent :
- la diffusion en temps réel des changements d'état des commandes et des paiements via STOMP (`/ws`, `/topic/*`),
- et la fourniture de l'`id` utilisateur dans la réponse d'authentification pour faciliter le comportement côté frontend.

(Section ajoutée automatiquement — aucune autre partie du fichier n'a été modifiée.)

