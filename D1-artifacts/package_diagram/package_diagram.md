@startuml KUDefenseTower_PackageDiagram

skinparam packageStyle rectangle
title KUDefenseTower - Logical Architecture (Package Diagram)

'---------------- UI Layer ----------------
package "UI" {
  [MainMenuScreen]
  [OptionsScreen]
  [MapEditorScreen]
  [GameScreen]
  [TowerPopup]
}

'---------------- Domain Layer ----------------
package "Domain" {
  package "model.map" {
    [Map]
    [Tile]
    [Path]
    [TowerSlot]
  }

  package "model.entities" {
    [Enemy]
    [Goblin]
    [Knight]
  }

  package "model.projectiles" {
    [Arrow]
    [Spell]
    [ArtilleryShell]
  }

  package "model.towers" {
    [Tower]
    [ArcherTower]
    [MageTower]
    [ArtilleryTower]
  }

  package "model.game" {
    [Player]
    [GameSession]
    [GameOptions]
  }

  package "controller" {
    [GameController]
    [MapEditorController]
    [OptionsController]
  }
}

'---------------- Technical Services Layer ----------------
package "Technical Services" {
  package "persistence" {
    [MapSerializer]
    [OptionsSerializer]
    [FileManager]
  }

  package "util" {
    [PathFinder]
    [GameClock]
    [MathUtils]
    [ResourceLoader]
  }
}

'---------------- Dependencies ----------------
"UI" ..> "Domain" : observes
"Domain" ..> "Technical Services" : uses
"controller" ..> "UI" : updates
"controller" ..> "model" : manipulates
"controller" ..> "persistence" : loads/saves

@enduml