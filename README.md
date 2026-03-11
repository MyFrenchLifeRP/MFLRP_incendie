# Plugin_MFLRP

Plugin Minecraft 1.12.2 pour serveur MFLRP - Simulation d'incendies

Ce plugin permet de simuler des incendies dans Minecraft autour de la position d'un joueur. Les paramètres (hauteur, taille, nom) sont fournis lors de l'exécution de la commande. Les feux se propagent automatiquement dans le temps et ne détruisent pas les blocs du monde.

## Fonctionnalités

- **Simulation d'incendie** : Crée des feux dans une zone circulaire autour de la position du joueur qui lance la commande
- **Paramètres dynamiques** : Le nom de la zone, la hauteur minimale/maximale et la taille maximale sont fournis par le joueur
- **Propagation automatique** : Le rayon du feu augmente linéairement pendant 30 minutes jusqu'à la taille maximale
- **Protection des blocs** : Les feux n'endommagent pas les structures existantes
- **Alertes** : Notification automatique à tous les joueurs lors du démarrage d'un incendie
- **Maintenance** : Les feux sont entretenus automatiquement toutes les minutes (sans s'éteindre entre-temps)

## Dépendances

- **Spigot/Bukkit** : Serveur Minecraft compatible

## Structure du projet

```
Plugin_MFLRP/
├── src/
│   ├── java/
│   │   └── fr/
│   │       └── incendie/
│   │           ├── Main.java
│   │           ├── StartFireCommand.java
│   │           ├── FireZone.java
│   │           └── FireListener.java
│   └── resources/
│       └── plugin.yml
├── lib/  (optionnel, ne plus nécessaire pour le plugin)
├── pom.xml
└── README.md
```

## Commandes

### /startfire

**Description** : Démarre un incendie autour du joueur avec paramètres personnalisés.

**Utilisation** :

```
/startfire <nom> <hauteurMin> <hauteurMax> <tailleMax> <vitessePropagation> <delaiReignition>
```

**Conditions** :

- Le joueur doit exécuter la commande et avoir la permission `plugin.startfire`

**Paramètres** :

- `<nom>` : identifiant de la zone d'incendie (affiché dans les alertes)
- `<hauteurMin>` et `<hauteurMax>` : limites verticales pour l'apparition du feu
- `<tailleMax>` : rayon maximal de la zone (minimum 3)
- `<vitessePropagation>` : délai en secondes entre chaque nouvelle flamme (ex : `30` = 1 flamme toutes les 30 secondes)
- `<delaiReignition>` : délai en secondes avant que le feu se redéclenche automatiquement après avoir été entièrement maîtrisé (ex : `300` = redéclenchement après 5 minutes)

**Effets** :

- Feu initial d'une seule flamme au centre, placée sur le premier bloc solide dans la plage de hauteurs
- Propagation aléatoire bloc par bloc à partir du centre, en s'étendant organiquement dans toute la zone
- Alerte globale avec le nom et les coordonnées de la zone
- Chaque incendie se propage différemment même avec les mêmes paramètres

> ⚠️ Si le plugin ne trouve aucun bloc solide dans la plage de hauteurs spécifiée, aucun feu ne sera posé. Le joueur recevra un message d'avertissement dans ce cas.

**Exemple** :

```
/startfire foret 60 80 50 30 300
```

Crée une zone nommée "foret" entre les niveaux 60 et 80, rayon max 50 blocs, avec 1 nouvelle flamme toutes les 30 secondes, et redéclenchement automatique 5 minutes après extinction complète.

### /listfires

**Description** : Affiche la liste des zones d'incendie actives avec leurs coordonnées et rayons actuels.

**Utilisation** :

```
/listfires
```

### /removefire

**Description** : Supprime complètement une zone d'incendie existante par son nom. Le feu est éteint et n'apparaîtra plus.

**Utilisation** :

```
/removefire <nomZone>
```

### /extinguishitem

**Description** : Configure un type d'item (par son nom Material ou son ID numérique) et un délai en secondes avant extinction : un clic droit avec cet item éteint une seule flamme ciblée.

**Utilisation** :

```
/extinguishitem <materialId> <delaiSecondes>
```

- `<materialId>` peut être soit le nom `Material` Bukkit (ex. `STICK`, `IRON_INGOT`, `BLAZE_ROD`), soit son ID numérique legacy 1.12 (ex. `280` pour `STICK`).
- `<delaiSecondes>` définit le temps d'attente avant extinction.
- Si `<delaiSecondes>` vaut `0`, l'extinction est instantanée.
- Le joueur reçoit un message de confirmation.
- La configuration est globale au plugin et persiste après redémarrage/reload du serveur.

Une fois l'item défini, équipe‑le et fais un clic droit sur une flamme pour en éteindre une seule.

### /firebreakitem

**Description** : Configure un type d'item (par son nom Material ou son ID numérique) et un délai en secondes avant action : un clic droit avec cet item sur un bloc de terre ou d'herbe le convertit en **Farmland** pendant 15 minutes, créant ainsi un pare-feu naturel.

**Utilisation** :

```
/firebreakitem <materialId> <delaiSecondes>
```

- `<materialId>` peut être soit le nom `Material` Bukkit (ex. `STICK`, `BLAZE_ROD`), soit son ID numérique legacy 1.12 (ex. `280` pour `STICK`).
- `<delaiSecondes>` définit le temps d'attente avant que la conversion soit appliquée.
- Si `<delaiSecondes>` vaut `0`, la conversion est instantanée.

**Blocs compatibles** : `GRASS` (herbe), `DIRT` (terre / coarse dirt / podzol), `MYCEL` (mycélium), `GRASS_PATH` (chemin).

**Comportement** :

- Un clic droit sur un bloc compatible le convertit en Farmland (`SOIL`, id 60).
- Après 15 minutes, le bloc est automatiquement restauré dans son état d'origine.
- Impossible de lancer deux actions pare-feu en même temps avec le même joueur : il faut attendre la fin de l'action en cours.

### /firebreakstatus

**Description** : Affiche la configuration de pare-feu actuellement chargée (item + ID + délai en secondes).

**Utilisation** :

```
/firebreakstatus
```

**Description** : Affiche la configuration d'extinction actuellement chargée (item + ID + délai en secondes).

**Utilisation** :

```
/extinguishstatus
```

Permet de vérifier rapidement en jeu si le serveur a bien chargé la bonne configuration.

## Permissions

### plugin.startfire

**Description** : Permission pour utiliser la commande /startfire

**Par défaut** : op (administrateurs seulement)

**Attribution** :

```yaml
permissions:
  plugin.startfire:
    default: op
```

## Compilation

Pour compiler le plugin avec Maven :

```bash
mvn clean package
```

Le fichier JAR compilé se trouvera dans le dossier `target/`.

## Installation

1. Compilez le plugin avec Maven
2. Copiez le fichier JAR généré (`Plugin_MFLRP-1.0-SNAPSHOT.jar`) dans le dossier `plugins/` de votre serveur
3. Redémarrez ou rechargez le serveur avec `/reload`

## Configuration

Le plugin ne nécessite pas de fichier de configuration supplémentaire. Les paramètres sont définis via la commande : la zone démarre toujours avec un rayon de 3 et grandit linéairement pendant 30 minutes jusqu'à la `tailleMax` fournie par le joueur. La hauteur minimum/maximum est également fixée lors de l'appel.

## Développement

- **Version Minecraft** : 1.12.2
- **API** : Spigot/Bukkit
- **Java** : 8+
- **Build Tool** : Maven
- **Dépendances** : Spigot API 1.12.2

## Notes techniques

- Les feux sont placés sur les blocs solides les plus élevés dans la plage de hauteurs définie
- La propagation est **aléatoire** : chaque incendie se répand différemment, bloc par bloc, à partir du centre jusqu'à remplir toute la zone
- Le système empêche la destruction des blocs par le feu naturel de Minecraft
- Le feu ne disparaît pas naturellement (extinction bloquée par le plugin, dure indéfiniment)
- Les incendies sont automatiquement éteints lors de l'arrêt du plugin
- Le scheduler vérifie la propagation toutes les secondes et rafraîchit les flammes toutes les 60 secondes
