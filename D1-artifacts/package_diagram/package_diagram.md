@startuml KUDefenseTower_PackageDiagram

skinparam packageStyle rectangle
title KUDefenseTower - Logical Architecture (Package Diagram)

' ---------------- UI Layer ----------------
package "UI" {
  package "Swing" {
    [MainMenuScreen]
    [OptionsScreen]
    [MapEditorScreen]
    [GameScreen]
  }

  
}

' ---------------- Domain Layer ----------------
package "Domain" {
  package "Game Entities" {
    [Goblin]
    [Knight]
    [Wave]
    [Group]
    [Arrow]
    [Spell]
    [ArtilleryShell]
    [ArcherTower]
    [MageTower]
    [ArtilleryTower]
    [TowerSlot]
    [Player]
  }

  [GamePlay Rules]
  [Game Mechanics]
  [State and Progress Management]
}

' ---------------- Technical Services Layer ----------------
package "Technical Service" {
  [MapSerializer]
  [OptionsSerializer]
  [FileManager]
  [PathFinder]
  [GameClock]
  [MathUtils]
  [ResourceLoader]
}

' ---------------- Dependencies ----------------
"UI" ..> "Domain"
"Domain" ..> "Technical Service"

@enduml