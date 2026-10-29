# Messagerie TP — version PC (Java Swing)

Application de bureau conçue à partir de tes cinq fichiers Android. Elle utilise **exactement le même backend Spring Boot** : aucun changement de route REST ou WebSocket n'est nécessaire.

## Prérequis

- JDK **17** ou plus récent.
- **Maven** installé (ou ouvre le `pom.xml` comme projet Maven dans IntelliJ IDEA).
- Ton serveur **Spring Boot** démarré sur le port `8080`.

## Démarrage

Dans un terminal, depuis le dossier `messagerie-pc` :

```bash
mvn clean package
java -jar target/messagerie-pc-1.0.0.jar
```

Ce projet contient le **code source**. Le `.jar` exécutable est généré sur ton ordinateur avec Maven ; il n'est pas inclus dans l'archive.

## Utilisation

1. Mets `http://localhost:8080` si Spring Boot tourne **sur le même PC**. S'il tourne ailleurs sur le réseau, indique l'adresse IP du serveur, par exemple `http://192.168.1.10:8080`.
2. Garde `/ws/messages` pour le chemin WebSocket.
3. Saisis **email** et **mot de passe**, puis clique sur **Se connecter**. Pour créer un compte, renseigne aussi **username** puis clique sur **S'inscrire**.
4. Clique sur **Actualiser les utilisateurs** puis sélectionne un interlocuteur. L'historique se charge automatiquement ; tu peux aussi cliquer sur **Charger l'historique**.
5. Saisis un message et clique sur **Envoyer** (ou Entrée). La livraison côté serveur n'est pas confirmée par l'application ; recharge l'historique pour voir les messages persistés.

## Routes utilisées (conservées)

| Opération | Méthode / URL | Données |
| --- | --- | --- |
| Inscription | `POST /api/comptes` | JSON : `email`, `username`, `mdp` |
| Connexion | `POST /api/auth/login` | JSON : `email`, `mdp` |
| Utilisateurs | `GET /api/utilisateurs` | Header : `Authorization: Bearer JWT` |
| Historique | `GET /api/messages/{utilisateurId}` | Header : `Authorization: Bearer JWT` |
| Temps réel | `ws://localhost:8080/ws/messages` | Header WebSocket : `Authorization: Bearer JWT` |

L'identifiant **de l'utilisateur connecté** vient du JWT ; l'identifiant **du destinataire** est dans l'URL pour l'historique et dans le JSON `destination_id` pour l'envoi WebSocket.

### Exemple de message WebSocket envoyé

```json
{"destination_id":14,"contenu":"Salut depuis mon PC !"}
```

### Exemple d'historique attendu du serveur

```json
[
  {"id":1,"expediteur_id":1,"destination_id":14,"contenu":"Salut","date":"2026-10-29T18:24:57"}
]
```

## Correspondance avec le projet Android

- `ApiClient.java` : repris de ton client Android, appels HTTP et parsing des réponses JSON.
- `AppConfig.java` : mêmes chemins REST/WS, `localhost` comme adresse par défaut sur PC.
- `DataParser.java` : mêmes noms de champs ; remplacement de `android.util.Base64` par `java.util.Base64`.
- `MessageSocket.java` : reprend ton WebSocket OkHttp avec le JWT dans le header ; ajout d'un arrêt propre de la connexion.
- `DesktopApp.java` : nouvelle interface Java Swing, remplace **MainActivity.java**, qui dépend du SDK Android.

## Diagnostic

- **404 sur l'historique** : vérifie le préfixe exact de ton `@RequestMapping` côté Spring Boot et que la route `GET /api/messages/{utilisateurId}` est exposée.
- **401 Unauthorized** : token manquant, expiré ou invalide ; reconnecte-toi.
- **Connection refused** : Spring Boot n'est pas démarré, le port est différent ou l'URL n'est pas accessible.
- **WebSocket déconnecté** : vérifie que l'intercepteur serveur accepte le JWT dans le header `Authorization` et que `/ws/messages` est la bonne route.
- **Historique ne montrant que les messages sortants** : vérifie la requête JPA côté serveur ; elle doit inclure `(expediteur = moi ET destinataire = autre) OU (expediteur = autre ET destinataire = moi)`.

Cette application n'est pas un nouvel **API serveur** : c'est un **client de bureau** compatible avec l'API Spring Boot existante.
