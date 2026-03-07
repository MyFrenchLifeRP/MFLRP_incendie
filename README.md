# Plugin_MFLRP

Plugin Minecraft 1.12.2 pour serveur MFLRP - Simulation d'incendies

Ce plugin permet de simuler des incendies dans Minecraft en utilisant WorldEdit pour définir les zones d'incendie. Les feux se propagent automatiquement dans le temps et ne détruisent pas les blocs du monde.

## Fonctionnalités

- **Simulation d'incendie** : Crée des feux dans une zone circulaire autour d'un point central
- **Propagation automatique** : Le rayon du feu augmente avec le temps (10 min → rayon 20, 20 min → rayon 30)
- **Protection des blocs** : Les feux n'endommagent pas les structures existantes
- **Alertes** : Notification automatique à tous les joueurs lors du démarrage d'un incendie
- **Maintenance** : Les feux sont entretenus automatiquement toutes les minutes

## Dépendances

- **WorldEdit** : Nécessaire pour sélectionner les zones d'incendie
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
├── lib/
│   └── we.jar (WorldEdit)
├── pom.xml
└── README.md
```

## Commandes

### /startfire
**Description** : Démarre un incendie dans la zone sélectionnée avec WorldEdit

**Utilisation** :
```
/startfire
```

**Conditions** :
- Le joueur doit avoir une sélection active avec WorldEdit (outil de sélection)
- Le joueur doit avoir la permission `plugin.startfire`

**Effets** :
- Crée un incendie au centre de la zone sélectionnée
- Rayon initial : 10 blocs
- Alerte tous les joueurs avec les coordonnées de l'incendie
- Le feu se propage automatiquement :
  - Après 10 minutes : rayon 20 blocs
  - Après 20 minutes : rayon 30 blocs

**Exemple** :
1. Sélectionnez une zone avec WorldEdit (//wand puis sélection)
2. Tapez `/startfire`
3. Un incendie démarre au centre de votre sélection

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

1. Assurez-vous que WorldEdit est installé sur votre serveur
2. Compilez le plugin avec Maven
3. Copiez le fichier JAR généré (`Plugin_MFLRP-1.0-SNAPSHOT.jar`) dans le dossier `plugins/` de votre serveur
4. Redémarrez ou rechargez le serveur avec `/reload`

## Configuration

Le plugin ne nécessite pas de fichier de configuration supplémentaire. Tous les paramètres sont codés en dur :

- Rayon initial : 10 blocs
- Expansion à 10 min : 20 blocs
- Expansion à 20 min : 30 blocs
- Intervalle de maintenance : 60 secondes

## Développement

- **Version Minecraft** : 1.12.2
- **API** : Spigot/Bukkit
- **Java** : 8+
- **Build Tool** : Maven
- **Dépendances** : Spigot API 1.12.2, WorldEdit 6.1.9

## Notes techniques

- Les feux sont placés sur les blocs solides les plus élevés dans le rayon défini
- Le système empêche la destruction des blocs par le feu naturel de Minecraft
- Les incendies sont automatiquement éteints lors de l'arrêt du plugin
- Le plugin utilise un scheduler pour maintenir et propager les feux
