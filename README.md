# AtlasRH

[![Java 17](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Spring Boot 4.0.9](https://img.shields.io/badge/Spring%20Boot-4.0.9-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-supported-2496ED?logo=docker&logoColor=white)](https://www.docker.com/)

AtlasRH est une API REST de gestion des ressources humaines développée dans le cadre de la formation YouCode – UM6P. Elle permet de gérer les départements, les employés, leurs contrats de travail ainsi que le workflow des demandes de congé.

Le projet utilise le package principal ~~~ma.atlasrh~~~ et expose une API documentée avec OpenAPI/Swagger.

## Fonctionnalités

Les fonctionnalités actuellement implémentées sont les suivantes :

- gestion des départements : création, modification, consultation, pagination et suppression d’un département vide ;
- gestion des employés : création, modification, consultation, recherche paginée, consultation de l’équipe d’un manager et calcul dynamique de l’ancienneté ;
- gestion des contrats : création, consultation de l’historique et terminaison d’un contrat ;
- création d’une demande de congé avec calcul des jours ouvrés, contrôle du chevauchement et du solde disponible ;
- consultation paginée des demandes de congé de l’employé identifié, avec filtre optionnel par statut ;
- consultation des demandes ~~~EN_ATTENTE~~~ des employés d’une équipe par son manager ;
- annulation d’une demande en attente par son propriétaire ;
- approbation ou refus d’une demande en attente par le manager responsable ;
- consultation du solde calculé pour chaque type de congé ;
- gestion centralisée des erreurs et validation des données entrantes.

L’identification de l’employé ou du manager pour les fonctionnalités concernées est réalisée par l’en-tête HTTP ~~~X-Employee-Id~~~. Le projet ne configure pas Spring Security ni JWT.

## Technologies utilisées

- Java 17 ;
- Spring Boot 4.0.9 ;
- Spring MVC ;
- Spring Data JPA et Hibernate ;
- PostgreSQL 16 ;
- Liquibase pour la gestion du schéma et des données de démonstration ;
- MapStruct 1.6.3 pour le mapping Entity/DTO ;
- Bean Validation ;
- Springdoc OpenAPI / Swagger UI ;
- Maven ;
- Docker et Docker Compose.

## Architecture

L’application suit une architecture en couches organisée dans ~~~ma.atlasrh~~~ :

| Package | Responsabilité |
| --- | --- |
| ~~~controller~~~ | Exposition des endpoints REST et délégation aux services |
| ~~~dto.request~~~ | Objets de requête validés |
| ~~~dto.response~~~ | Objets retournés par l’API |
| ~~~entity~~~ | Entités persistées par JPA |
| ~~~enums~~~ | Statuts et types métier |
| ~~~mapper~~~ | Conversion entre entités et DTO avec MapStruct |
| ~~~repository~~~ | Accès aux données avec Spring Data JPA |
| ~~~service~~~ | Règles métier et transactions |
| ~~~exception~~~ | Exceptions métier et gestionnaire global des erreurs |
| ~~~config~~~ | Configuration OpenAPI |

Les contrôleurs ne retournent pas directement les entités JPA : les réponses sont exposées au moyen de DTO.

## Modèle de données

Le schéma Liquibase crée les principales tables suivantes :

- ~~~department~~~ : département de l’entreprise ;
- ~~~employee~~~ : employé, rattaché à un département et pouvant avoir un manager qui est lui-même un employé ;
- ~~~contract~~~ : contrat associé à un employé, avec type, période, salaire et statut ;
- ~~~leave_type~~~ : type de congé et nombre de jours autorisés par an ;
- ~~~leave_request~~~ : demande associée à un employé et à un type de congé, avec période, nombre de jours, statut et commentaire du manager.

Les relations principales sont les suivantes :

~~~text
Department 1 ──── * Employee
Employee   1 ──── * Contract
Employee   1 ──── * LeaveRequest
LeaveType  1 ──── * LeaveRequest
Employee   1 ──── * Employee (manager)
~~~

Le schéma et les données de démonstration sont définis dans ~~~src/main/resources/db/changelog~~~.

## Prérequis

Pour un lancement local avec Maven :

- Java 17 ou supérieur ;
- PostgreSQL accessible sur ~~~localhost:5432~~~ ;
- une base ~~~atlasrh~~~ ;
- un utilisateur PostgreSQL ~~~atlasrh~~~ avec le mot de passe ~~~atlasrh~~~, ou des paramètres adaptés via variables d’environnement.

Pour un lancement avec Docker, seul Docker Desktop ou Docker Engine avec Docker Compose est nécessaire.

## Installation et configuration

Cloner le projet puis se placer à sa racine :

~~~bash
git clone <URL_DU_DEPOT>
cd atlasrh
~~~

La configuration par défaut est définie dans ~~~src/main/resources/application.yaml~~~ :

~~~text
DB_URL=jdbc:postgresql://localhost:5432/atlasrh
DB_USER=atlasrh
DB_PASSWORD=atlasrh
~~~

Ces valeurs peuvent être remplacées sans modifier le code :

~~~bash
DB_URL=jdbc:postgresql://localhost:5432/atlasrh \
DB_USER=mon_utilisateur \
DB_PASSWORD=mon_mot_de_passe \
./mvnw spring-boot:run
~~~

Liquibase applique automatiquement le changelog configuré au démarrage. Hibernate est configuré avec ~~~ddl-auto: validate~~~ : la structure de la base est donc gérée par Liquibase.

## Lancement

### Avec Maven et PostgreSQL local

Compiler le projet :

~~~bash
./mvnw clean package
~~~

Lancer l’application :

~~~bash
./mvnw spring-boot:run
~~~

Sous Windows, les commandes équivalentes utilisent ~~~mvnw.cmd~~~ :

~~~powershell
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
~~~

L’API est disponible sur http://localhost:8080.

### Avec Docker Compose

Le fichier ~~~docker-compose.yml~~~ démarre PostgreSQL et l’application avec une configuration cohérente entre les deux conteneurs :

~~~bash
docker compose up --build
~~~

Arrêter les services :

~~~bash
docker compose down
~~~

Les données PostgreSQL sont conservées dans le volume Docker ~~~atlasrh-db-data~~~.

## Documentation API

Après le démarrage de l’application :

- Swagger UI : [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- Spécification OpenAPI JSON : [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

## Endpoints REST

Toutes les réponses sont des DTO. Les endpoints marqués avec ~~~X-Employee-Id~~~ nécessitent cet en-tête, car aucun mécanisme d’authentification n’est configuré dans le projet.

### Départements

| Méthode | Route | Description |
| --- | --- | --- |
| ~~~POST~~~ | ~~~/api/departments~~~ | Créer un département |
| ~~~PUT~~~ | ~~~/api/departments/{id}~~~ | Modifier un département |
| ~~~GET~~~ | ~~~/api/departments/{id}~~~ | Consulter un département |
| ~~~GET~~~ | ~~~/api/departments~~~ | Lister les départements avec pagination et tri |
| ~~~DELETE~~~ | ~~~/api/departments/{id}~~~ | Supprimer un département vide |

### Employés

| Méthode | Route | Description |
| --- | --- | --- |
| ~~~POST~~~ | ~~~/api/employees~~~ | Créer un employé |
| ~~~PUT~~~ | ~~~/api/employees/{id}~~~ | Modifier un employé |
| ~~~GET~~~ | ~~~/api/employees/{id}~~~ | Consulter un employé |
| ~~~GET~~~ | ~~~/api/employees~~~ | Rechercher des employés ; paramètres optionnels ~~~search~~~, ~~~departmentId~~~, ~~~page~~~, ~~~size~~~, ~~~sort~~~ |
| ~~~GET~~~ | ~~~/api/employees/{id}/team~~~ | Consulter l’équipe d’un manager avec pagination |
| ~~~GET~~~ | ~~~/api/employees/me/seniority~~~ | Calculer mon ancienneté ; en-tête ~~~X-Employee-Id~~~ requis |

### Contrats

| Méthode | Route | Description |
| --- | --- | --- |
| ~~~POST~~~ | ~~~/api/employees/{employeeId}/contracts~~~ | Créer un contrat |
| ~~~GET~~~ | ~~~/api/employees/{employeeId}/contracts~~~ | Consulter l’historique des contrats |
| ~~~PUT~~~ | ~~~/api/contracts/{contractId}/terminate~~~ | Terminer un contrat |

### Demandes de congé

| Méthode | Route | Description |
| --- | --- | --- |
| ~~~POST~~~ | ~~~/api/leave-requests~~~ | Créer une demande ; en-tête ~~~X-Employee-Id~~~ requis |
| ~~~GET~~~ | ~~~/api/leave-requests/me~~~ | Consulter mes demandes ; filtre optionnel ~~~status~~~, pagination et tri |
| ~~~GET~~~ | ~~~/api/leave-requests/team/pending~~~ | Consulter les demandes en attente de mon équipe ; en-tête ~~~X-Employee-Id~~~ requis |
| ~~~PATCH~~~ | ~~~/api/leave-requests/{id}/cancel~~~ | Annuler sa propre demande en attente |
| ~~~PATCH~~~ | ~~~/api/leave-requests/{id}/approve~~~ | Approuver une demande de son équipe |
| ~~~PATCH~~~ | ~~~/api/leave-requests/{id}/reject~~~ | Refuser une demande avec commentaire obligatoire |

Pour créer une demande, le corps utilise l’identifiant du type de congé :

~~~json
{
  "leaveTypeId": 1,
  "startDate": "2026-11-02",
  "endDate": "2026-11-06",
  "reason": "Congé annuel"
}
~~~

Les statuts disponibles sont ~~~EN_ATTENTE~~~, ~~~APPROUVEE~~~, ~~~REFUSEE~~~ et ~~~ANNULEE~~~. Les jours sont calculés du lundi au vendredi, dates incluses. Les demandes en attente ou approuvées sont considérées comme actives lors du contrôle du chevauchement et du solde à la création.

### Soldes de congé

| Méthode | Route | Description |
| --- | --- | --- |
| ~~~GET~~~ | ~~~/api/leave-balances/me~~~ | Calculer le solde de chaque type de congé ; en-tête ~~~X-Employee-Id~~~ requis |

Le solde est calculé à partir des jours annuels du type de congé et des demandes approuvées de l’année courante. Aucun solde restant n’est stocké comme donnée persistante.

### Pagination et tri

Les endpoints paginés acceptent les paramètres Spring Data habituels :

~~~text
?page=0&size=10&sort=lastName,asc
~~~

Les valeurs par défaut dépendent de chaque endpoint et sont visibles dans les contrôleurs.

## Gestion des erreurs

~~~GlobalExceptionHandler~~~ centralise les erreurs et retourne un objet JSON homogène contenant notamment :

~~~json
{
  "timestamp": "2026-10-30T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Les données envoyées sont invalides.",
  "path": "/api/leave-requests"
}
~~~

Les principaux cas gérés sont :

- ~~~400 Bad Request~~~ : validation, date invalide, type de requête incorrect ou en-tête obligatoire manquant ;
- ~~~404 Not Found~~~ : ressource inexistante ;
- ~~~409 Conflict~~~ : doublon, règle métier non respectée ou contrainte d’intégrité ;
- ~~~500 Internal Server Error~~~ : erreur inattendue.

Les erreurs de validation peuvent inclure une propriété ~~~errors~~~ détaillant les champs invalides.

## Structure du projet

~~~text
atlasrh/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── mvnw
├── mvnw.cmd
├── src/
│   ├── main/
│   │   ├── java/ma/atlasrh/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── mapper/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── application.yaml
│   │       └── db/changelog/
│   └── test/
│       └── java/ma/atlasrh/
└── scripts/
~~~

## Tests

Exécuter les tests avec Maven :

~~~bash
./mvnw test
~~~

Les tests nécessitent une configuration PostgreSQL accessible avec les paramètres définis dans ~~~application.yaml~~~ sauf surcharge par variables d’environnement.

## Auteur

À compléter : **Nom de l’auteur / équipe**

Formation : **YouCode – UM6P**
