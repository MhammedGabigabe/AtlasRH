# Résultats des tests - Livrable 1

Exécution : 2026-10-29 09:39:08 - API : http://localhost:8080

| N° | Test | Attendu | Obtenu | OK/KO |
|---:|---|---|---|:---:|
| 1 | GET /api/departments | 200 + réponse paginée + 3 départements | 200 + paginée=True + totalElements=3 | OK |
| 2 | POST /api/departments Marketing | 201 + id renvoyé | 201 + id=8 + name=Marketing | OK |
| 3 | POST /api/departments Marketing (doublon) | 409 + JSON d'erreur conforme | 409 - Un département avec le nom 'Marketing' existe déjà. ; format JSON OK | OK |
| 4 | POST /api/departments nom vide | 400 + champ errors | 400 - Les données envoyées sont invalides. ; format JSON OK | OK |
| 5 | PUT département Marketing (nouveau nom) | 200 + nom mis à jour | 200 + name=Marketing Test | OK |
| 6 | PUT département Marketing -> nom Finance | 409 + JSON d'erreur conforme | 409 - Un département avec le nom 'Finance' existe déjà. ; format JSON OK | OK |
| 7 | PUT /api/departments/999 | 404 + JSON d'erreur conforme | 404 - Département introuvable avec l'id 999. ; format JSON OK | OK |
| 8 | GET /api/departments/{id Marketing} | 200 + département Marketing Test | 200 + name=Marketing Test | OK |
| 9 | GET /api/departments/999 | 404 + JSON d'erreur conforme | 404 - Département introuvable avec l'id 999. ; format JSON OK | OK |
| 10 | DELETE département Informatique (non vide) | 409 + JSON d'erreur conforme | 409 - Impossible de supprimer le département 'Informatique' car il contient encore des employés. ; format JSON OK | OK |
| 11 | DELETE département Marketing (vide) | 200 + message JSON | 200 + message=True | OK |
| 12 | GET département Marketing supprimé | 404 + JSON d'erreur conforme | 404 - Département introuvable avec l'id 8. ; format JSON OK | OK |
| 13 | GET /api/employees?search=ala | 200 + contient Alaoui | 200 + 0 résultat(s) :  | KO |
| 14 | GET /api/employees?search=ben (contrôle recherche) | 200 + contient Benali | 200 + 1 résultat(s) : Salma Benali | OK |
| 15 | GET /api/employees (sans paramètre) | 200 + page d'employés | 200 + totalElements=8 | OK |
| 16 | GET /api/employees?search= (chaîne vide) | 200, aucune erreur PostgreSQL | 200 + totalElements=8 | OK |
| 17 | GET employés département Informatique triés lastName asc | 200 + uniquement dept 2 + tri croissant | 200 + deptOK=True + triOK=True + Benali, Fassi, Ouazzani | OK |
| 18 | GET /api/employees?sort=abc | 400 + JSON d'erreur conforme | 400 - Le champ de tri 'abc' n'existe pas. ; format JSON OK | OK |
| 19 | POST employé EMP009 valide | 201 + id + matricule EMP009 | 201 + id=14 + matricule=EMP009 + managerId=2 | OK |
| 20 | POST employé matricule EMP009 déjà utilisé | 409 + JSON d'erreur conforme | 409 - Un employé avec le matricule 'EMP009' existe déjà. ; format JSON OK | OK |
| 21 | POST employé e-mail déjà utilisé (autre matricule) | 409 + JSON d'erreur conforme | 409 - Un employé avec l'e-mail 'gabi.test@atlasrh.ma' existe déjà. ; format JSON OK | OK |
| 22 | POST employé departmentId=999 | 404 + JSON d'erreur conforme | 404 - Département introuvable avec l'id 999. ; format JSON OK | OK |
| 23 | POST employé managerId=999 | 404 + JSON d'erreur conforme | 404 - Manager introuvable avec l'id 999. ; format JSON OK | OK |
| 24 | POST employé e-mail invalide | 400 + champ errors | 400 - Les données envoyées sont invalides. ; format JSON OK | OK |
| 25 | POST employé sans hireDate | 400 + champ errors | 400 - Les données envoyées sont invalides. ; format JSON OK | OK |
| 26 | POST employé hireDate invalide | 400 + JSON d'erreur conforme | 400 - Le corps de la requête est invalide ou mal formé (JSON, énumération ou date incorrecte). ; format JSON OK | OK |
| 27 | PUT EMP004 avec managerId = son propre id | 400 + JSON d'erreur conforme | 400 - Un employé ne peut pas être son propre manager. ; format JSON OK | OK |
| 28 | PUT EMP004 sans managerId | 200 + managerId null | 200 + managerId= | OK |
| 29 | PUT EMP004 remise du manager d'origine | 200 + managerId=1 | 200 + managerId=1 | OK |
| 30 | GET équipe EMP002 (attendu du brief) | 200 + EMP004, EMP005, EMP008 (+ EMP009) | 200 + EMP006, EMP007, EMP009 | KO |
| 31 | GET équipe EMP002 (cohérence manager_id=2) | 200 + EMP006, EMP007, EMP009 | 200 + EMP006, EMP007, EMP009 | OK |
| 32 | GET /api/employees/999 | 404 + JSON d'erreur conforme | 404 - Employé introuvable avec l'id 999. ; format JSON OK | OK |
| 33 | GET /api/employees/999/team | 404 + JSON d'erreur conforme | 404 - Manager introuvable avec l'id 999. ; format JSON OK | OK |
| 34 | GET /api/employees/abc | 400 + JSON d'erreur conforme | 400 - Le paramètre 'id' a une valeur invalide : abc ; format JSON OK | OK |
| 35 | GET contrats EMP005 | 200 + 2 contrats (CDD TERMINE + CDI EN_COURS), récent d'abord | 200 + 1 contrat(s) : CDI EN_COURS | KO |
| 36 | GET contrats EMP004 (contrôle données démo) | 200 + 2 contrats, récent d'abord | 200 + CDI EN_COURS, CDD TERMINE + ordreOK=True | OK |
| 37 | POST CDI EMP004 alors qu'un contrat est en cours | 409 + JSON d'erreur conforme | 409 - L'employé 'EMP004' a déjà un contrat en cours. ; format JSON OK | OK |
| 38 | PUT terminer le CDI en cours d'EMP004 | 200 + statut TERMINE + endDate=2026-10-29 | 200 + statut=TERMINE + endDate=2026-10-29 | OK |
| 39 | PUT terminer à nouveau le même contrat | 409 + JSON d'erreur conforme | 409 - Le contrat 5 est déjà terminé. ; format JSON OK | OK |
| 40 | PUT /api/contracts/999/terminate | 404 + JSON d'erreur conforme | 404 - Contrat introuvable avec l'id 999. ; format JSON OK | OK |
| 41 | POST CDD sans endDate | 400 + JSON d'erreur conforme | 400 - Un contrat CDD ou STAGE doit avoir une date de fin. ; format JSON OK | OK |
| 42 | POST CDD endDate antérieure à startDate | 400 + JSON d'erreur conforme | 400 - La date de fin ne peut pas être antérieure à la date de début. ; format JSON OK | OK |
| 43 | POST contrat type CDX | 400 + JSON d'erreur conforme | 400 - Le corps de la requête est invalide ou mal formé (JSON, énumération ou date incorrecte). ; format JSON OK | OK |
| 44 | POST contrat grossSalary = -5 | 400 + champ errors | 400 - Les données envoyées sont invalides. ; format JSON OK | OK |
| 45 | POST contrat sans grossSalary | 400 + champ errors | 400 - Les données envoyées sont invalides. ; format JSON OK | OK |
| 46 | POST CDD valide pour EMP004 | 201 + statut EN_COURS | 201 + id=15 + statut=EN_COURS | OK |
| 47 | POST deuxième contrat juste après | 409 + JSON d'erreur conforme | 409 - L'employé 'EMP004' a déjà un contrat en cours. ; format JSON OK | OK |
| 48 | POST contrat pour employé 999 | 404 + JSON d'erreur conforme | 404 - Employé introuvable avec l'id 999. ; format JSON OK | OK |
| 49 | GET contrats employé 999 | 404 + JSON d'erreur conforme | 404 - Employé introuvable avec l'id 999. ; format JSON OK | OK |

Total : 49 tests - OK : 46 - KO : 3
