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
/startfire <nom> <hauteurMin> <hauteurMax> <tailleMax>
```

**Conditions** :
- Le joueur doit exécuter la commande et avoir la permission `plugin.startfire`

**Paramètres** :
- `<nom>` : identifiant de la zone d'incendie (affiché dans les alertes)
- `<hauteurMin>` et `<hauteurMax>` : limites verticales pour l'apparition du feu
- `<tailleMax>` : rayon final (minimum 3) après 30 minutes de propagation

**Effets** :
- Feu initial de rayon 3 autour du joueur
- Alerte globale avec le nom et les coordonnées de la zone
- Le rayon augmente progressivement jusqu'à `tailleMax` sur 30 minutes

> ⚠️ Si le plugin ne trouve aucun bloc solide dans la plage de hauteurs spécifiée, aucun feu ne sera posé. Le joueur recevra un message d'avertissement dans ce cas.

**Exemple** :
```
/startfire foret 60 80 50
```
crée une zone nommée "foret" entre les niveaux 60 et 80, montant jusqu'à 50 blocs de rayon.

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

- Les feux sont placés sur les blocs solides les plus élevés dans le rayon défini
- Le système empêche la destruction des blocs par le feu naturel de Minecraft
- Le feu ne disparaît pas naturellement (extinction bloquée par le plugin, dure indéfiniment)
- Les incendies sont automatiquement éteints lors de l'arrêt du plugin
- Le plugin utilise un scheduler pour maintenir et propager les feux (sans les éteindre entre les cycles)
