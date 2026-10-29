# Client Android Java pour le TP Messagerie Instantanée

Application Android **native en Java**. Aucun serveur n'est inclus : le client se branche sur **ton serveur existant**.

## Ce que fait l'application

- `POST /api/comptes` : inscription.
- `POST /api/auth/login` : connexion, récupération d'un JWT.
- `GET /api/utilisateurs` : liste des utilisateurs avec leur statut.
- `GET /api/messages/{utilisateurId}` : récupération de l'historique.
- WebSocket `/ws/messages` : échange de messages en temps réel.
- Compatible avec un vrai téléphone Android (Android 7.0+), un émulateur Android ou plusieurs téléphones.

L'interface est construite directement dans `MainActivity.java` : **aucun fichier XML de mise en page nécessaire**.

## Fichiers Java

- `app/src/main/java/fr/tp/messagerie/MainActivity.java` : interface Android, inscription, login, liste, historique, chat.
- `app/src/main/java/fr/tp/messagerie/ApiClient.java` : appels HTTP REST.
- `app/src/main/java/fr/tp/messagerie/MessageSocket.java` : connexion WebSocket avec OkHttp.
- `app/src/main/java/fr/tp/messagerie/DataParser.java` : lecture des réponses JSON, récupération du JWT et des identifiants.
- `app/src/main/java/fr/tp/messagerie/AppConfig.java` : endpoints API, URL WebSocket et mode de construction de l'URL.

## Générer l'APK dans Android Studio

1. Décompresse `ClientMessagerieAndroid.zip`.
2. Ouvre **Android Studio** avec un JDK 17.
3. Fais **File > Open** et sélectionne le dossier `ClientMessagerieAndroid` contenant `settings.gradle`.
4. Laisse Android Studio synchroniser Gradle, installer l'Android SDK 35 et télécharger les dépendances (Internet nécessaire).
5. Le projet cible Android Gradle Plugin 8.7.3 et **Gradle 8.9**. **Cette archive ne contient pas le Gradle Wrapper JAR** : si Android Studio le réclame, crée le wrapper avec un Gradle installé (`gradle wrapper --gradle-version 8.9`), ou crée un projet Android Studio Java/Empty Views Activity et copie les fichiers `app/src/main/java/...`, `AndroidManifest.xml` et les paramètres `app/build.gradle` de cette archive.
6. Pour produire un APK : **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
7. L'APK debug se trouve normalement dans `app/build/outputs/apk/debug/app-debug.apk`.
8. Copie cet APK sur ton téléphone Android et installe-le (autorise l'installation depuis cette source si Android le demande).

**Attention :** l'APK n'a pas été compilé ici (pas de SDK Android dans l'environnement de génération). Les fichiers Java ont fait l'objet d'un contrôle syntaxique, mais il faut les construire et les tester avec ton backend dans Android Studio.

## Connexion au serveur : téléphone réel vs émulateur

- **Émulateur Android** sur le même PC que le backend : URL préremplie `http://10.0.2.2:8080`.
- **Téléphone réel** sur le Wi-Fi de ton PC : remplace l'URL par `http://192.168.1.XX:8080` avec **l'adresse IP locale réelle de ton PC** et le port réel de ton backend.
- `localhost` et `127.0.0.1` sur un téléphone désignent le **téléphone**, pas ton PC.
- Assure-toi que les deux appareils sont sur le même réseau, que le serveur écoute sur une interface accessible (pas seulement localhost), que le pare-feu autorise le port et que les routes API sont atteignables depuis le téléphone.

## Adapter le client à TON serveur

Le sujet de TP fournit des routes, mais **ne précise pas tous les formats JSON de réponses**. Les hypothèses du client sont donc à vérifier :

- Les chemins REST et WebSocket sont configurés dans **`AppConfig.java`**. Exemple : si ton backend utilise `/api/register` au lieu de `/api/comptes`, modifie `REGISTER_PATH`.
- Le nom du champ de mot de passe des requêtes POST est configurable à l'écran : `password` par défaut, ou `motDePasse` si c'est ce qu'attend le backend.
- Le login est envoyé sous forme `{"username":"alice","password":"secret"}` (ou `motDePasse` selon ton réglage).
- La réponse de login doit contenir un token sous `token`, `accessToken`, `access_token`, `jwt` ou `jwtToken`, éventuellement dans `data` ou `result`. Sinon, adapte `DataParser.token()`.
- Le client retrouve ton ID dans la réponse de login, certaines claims JWT ou la liste des utilisateurs. Sinon, renseigne l'ID dans le champ facultatif **Mon ID** de la zone paramètres.
- La liste des utilisateurs est un tableau JSON `[...]` ou un objet avec un tableau sous `utilisateurs`, `users`, `data`, `results` ou `content`. Chaque utilisateur doit avoir un `id` et idéalement `username`.
- L'historique est un tableau JSON direct ou un tableau sous `messages`, `historique`, `data`, `results` ou `content`.
- **JWT WebSocket** : par défaut, transmis dans `?token=JWT`; décoche la case si ton serveur attend `Authorization: Bearer JWT` pendant le handshake. Si ton backend utilise un autre nom de paramètre, modifie `WEBSOCKET_TOKEN_PARAMETER`.

### Format JSON envoyé par WebSocket

```json
{
  "type": "message",
  "expediteurId": 1,
  "destinataireId": 2,
  "contenu": "Bonjour !"
}
```

`dateEnvoi` n'est **pas** générée par le client : **c'est le serveur** qui doit horodater et persister le message, puis le retransmettre. Les messages entrants peuvent comporter `dateEnvoi`.

**Sécurité côté serveur :** ne fais pas confiance à `expediteurId` transmis par le client. Vérifie le JWT et déduis l'expéditeur authentifié côté serveur ; protège aussi la récupération de l'historique.

## Utilisation

1. Lance le backend, puis ouvre l'application sur ton téléphone.
2. Renseigne l'URL du backend, le nom du champ de mot de passe et le mode JWT pour WebSocket.
3. Saisis ton identifiant et mot de passe, puis clique sur **Se connecter** ou **S'inscrire**.
4. Après le login, le client récupère la liste des utilisateurs et ouvre une connexion WebSocket.
5. Sélectionne un utilisateur, charge l'historique avec le bouton prévu, puis envoie un message.
6. Sur un autre appareil (ou ton client web existant), connecte un autre compte pour vérifier l'échange bidirectionnel.

## Notes et limites

- Le client **ne sauvegarde pas les messages en local** : ils doivent être sauvegardés par le serveur et récupérés via `GET /api/messages/{utilisateurId}`.
- Le JWT n'est **pas stocké sur le disque** ; il reste en mémoire pendant la session. Le mot de passe n'est pas enregistré.
- L'écran affiche « transmis au canal » quand l'envoi WebSocket a été accepté par le client, **pas** comme preuve que le destinataire a reçu le message.
- Le projet autorise `http://` et `ws://` en clair pour les tests locaux du TP via `android:usesCleartextTraffic="true"`. Pour un déploiement public, utilise impérativement `https://` / `wss://` et désactive le trafic non chiffré.
- La reconnexion WebSocket automatique et les accusés de lecture ne sont pas implémentés dans ce client minimal.
- Si ton backend utilise STOMP ou SockJS **au lieu de WebSocket JSON brut**, ce client doit être adapté (protocole différent).
- Si l'application ne fonctionne pas immédiatement avec ton backend, les 3 éléments les plus utiles pour l'adapter sont : **la réponse JSON de login, le JSON de GET `/api/utilisateurs`, et le format exact des frames WebSocket**.
