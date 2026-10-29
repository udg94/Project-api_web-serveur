# Projet de messagerie instantanée — API REST et WebSocket

## Présentation

Ce projet est une application de messagerie instantanée permettant à deux utilisateurs d'échanger des messages en temps réel, depuis deux appareils distincts, par l'intermédiaire d'un serveur central.

Le serveur est développé en **Java avec Spring Boot**. Le projet comprend **deux clients** : une application **Android en Java**, compilable en APK, et une **version PC du client Android**. Les échanges classiques utilisent une API REST ; l'envoi et la réception instantanée des messages utilisent un WebSocket.

## Fonctionnalités

- Création d'un compte et connexion avec email et mot de passe.
- Authentification par jeton **JWT**.
- Consultation des utilisateurs, de leur identifiant et de leur statut.
- Sélection d'un destinataire par son compte, sans saisir son ID manuellement.
- Envoi et réception de messages texte en temps réel via **WebSocket**.
- Consultation de l'historique des conversations via l'API REST, lorsque cette route est disponible sur le serveur.
- Utilisation du client Android sur émulateur ou sur un téléphone réel.
- Utilisation d'un second client sur PC pour échanger des messages avec le client Android.

## Technologies

| Composant | Technologies |
| --- | --- |
| Serveur | Java, Spring Boot |
| API | HTTP, REST, JSON |
| Messages instantanés | WebSocket |
| Authentification | JWT |
| Stockage des messages | Base de données côté serveur |
| Client mobile | Java, Android Studio, Android SDK |
| Client PC | Version pour ordinateur adaptée du client Android (technologies à préciser selon les sources du client PC) |

## Architecture

```text
Téléphone Android (client A)               Client PC (client B)
             |                                 |
             +----------- Réseau --------------+
                              |
                  Serveur Spring Boot
                  |                |
           API REST HTTP       WebSocket
                  \                /
                   Base de données
```

Les clients ne communiquent pas directement : les messages passent par le serveur.

## Lancement du serveur

1. Ouvrir le projet **backend** dans l'environnement Java utilisé pour le développement.
2. Configurer la base de données dans les propriétés Spring Boot, si nécessaire.
3. Démarrer l'application Spring Boot.
4. Vérifier depuis le PC que `http://localhost:8080/api` répond (adapter le port si besoin).

## Lancement du client Android

1. Ouvrir le dossier du projet Android dans **Android Studio** (celui contenant `settings.gradle` et `app/`).
2. Synchroniser Gradle et sélectionner un émulateur ou un téléphone Android connecté.
3. Cliquer sur **Run** pour installer et lancer l'application.
4. Renseigner l'adresse du serveur :
   - **Depuis l'émulateur Android** : `http://10.0.2.2:8080`.
   - **Depuis un vrai téléphone** : `http://ADRESSE_IP_DU_PC:8080` (par exemple `http://192.168.1.20:8080`). Le téléphone et le PC doivent se trouver sur un réseau permettant la communication (ie parfeu déctativé).
5. Se connecter ou créer un compte, choisir un destinataire et envoyer un message.

Pour générer un APK dans Android Studio : **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Le fichier de test se trouve normalement dans `app/build/outputs/apk/debug/app-debug.apk`.

## Lancement du client PC

Le projet contient également **une version PC du client Android**, créée avec l'aide d'un autre échange avec une intelligence artificielle.

1. Ouvrir le dossier contenant les sources du client PC.
2. Lancer le client selon la procédure correspondant à sa technologie (à documenter dans ce dossier, si nécessaire).
3. Renseigner l'adresse du serveur Spring Boot, par exemple `http://localhost:8080` si le client PC et le serveur tournent sur le même ordinateur.
4. Se connecter avec un **compte différent** de celui du téléphone, puis tester les échanges dans les deux sens.
.

## Principales routes de l'API

| Méthode | Route | Rôle |
| --- | --- | --- |
| `POST` | `/api/comptes` | Création d'un compte (`email`, `username`, `mdp`). |
| `POST` | `/api/auth/login` | Connexion (`email`, `mdp`) et réception du JWT. |
| `GET` | `/api/utilisateurs` | Liste des utilisateurs, identifiants et statuts. |
| `GET` | `/api/messages/{utilisateurId}` | Historique de conversation (à exposer côté serveur). |

Les routes protégées utilisent l'en-tête `Authorization: Bearer <JWT>`.

### WebSocket

- **Chemin** : `/ws/messages`
- **Authentification** : JWT transmis dans l'en-tête `Authorization` lors de l'ouverture du WebSocket.
- **Exemple de message envoyé** :

```json
{
  "destination_id": 14,
  "contenu": "Bonjour depuis Android !"
}
```

Le serveur détermine l'identité de l'expéditeur à partir du JWT ; elle n'est pas fournie librement par le client.

## Démonstration

1. Démarrer le serveur Spring Boot.
2. Connecter un compte depuis le téléphone Android et un autre depuis le client PC, sur deux appareils distincts.
3. Envoyer un message du premier client vers le second et vérifier sa réception immédiate.
4. Effectuer le test dans l'autre sens.
5. Si la persistance et l'API d'historique sont activées, se déconnecter puis se reconnecter pour vérifier la récupération des échanges précédents.

## Déclaration d'utilisation de l'intelligence artificielle


**Les deux clients de ce projet ont été développés avec l'aide de l'intelligence artificielle.**

- **Client Android :** l'IA a aidé à générer et à adapter le code Java, l'interface et les appels REST/WebSocket à l'API Spring Boot du projet.
- **Client PC :** j'ai demandé, dans **un autre chat avec une IA**, de créer une **version PC du client Android**, afin de disposer d'un second client pour tester la messagerie entre deux appareils.

L'IA a été utilisée comme **outil d'assistance au développement des clients**. J'ai intégré et testé le client Android avec mon serveur, notamment sur émulateur et sur un téléphone réel. L'utilisation de l'IA pour le client PC est déclarée ici également. Cette déclaration ne signifie pas que l'IA a réalisé l'intégralité du projet ou le backend.